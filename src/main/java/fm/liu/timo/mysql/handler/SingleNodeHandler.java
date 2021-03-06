/*
 * Copyright 2015 Liu Huanting.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package fm.liu.timo.mysql.handler;

import java.util.List;
import fm.liu.messenger.Mail;
import fm.liu.timo.TimoServer;
import fm.liu.timo.config.model.Datasource;
import fm.liu.timo.mysql.connection.MySQLConnection;
import fm.liu.timo.mysql.packet.ErrorPacket;
import fm.liu.timo.mysql.packet.OkPacket;
import fm.liu.timo.net.connection.BackendConnection;
import fm.liu.timo.server.ServerConnection;
import fm.liu.timo.server.session.Session;
import fm.liu.timo.server.session.TransactionSession;
import fm.liu.timo.server.session.handler.SessionResultHandler;
import fm.liu.timo.statistic.SQLRecord;
import fm.liu.timo.util.TimeUtil;

/**
 * @author Liu Huanting 2015年5月9日
 */
public class SingleNodeHandler extends SessionResultHandler {
    private String sql;

    public SingleNodeHandler(Session session) {
        super.session = session;
    }

    @Override
    public void ok(byte[] data, BackendConnection con) {
        record(con);
        session.release(con);
        OkPacket ok = new OkPacket();
        ok.read(data);
        ok.packetId = ++packetId;
        if (session instanceof TransactionSession) {
            ((TransactionSession) session).savepoint(ok);
        } else {
            ok.write(session.getFront());
        }
    }

    @Override
    public void error(byte[] data, BackendConnection con) {
        session.release(con);
        ErrorPacket err = new ErrorPacket();
        err.read(data);
        err.packetId = ++packetId;
        err.write(session.getFront());
    }

    @Override
    public void field(byte[] header, List<byte[]> fields, byte[] eof, BackendConnection con) {
        header[3] = ++packetId;
        ServerConnection front = session.getFront();
        buffer = front.writeToBuffer(header, allocBuffer());
        for (int i = 0, len = fields.size(); i < len; ++i) {
            byte[] field = fields.get(i);
            field[3] = ++packetId;
            buffer = front.writeToBuffer(field, buffer);
        }
        eof[3] = ++packetId;
        buffer = front.writeToBuffer(eof, buffer);
    }

    @Override
    public void row(byte[] row, BackendConnection con) {
        row[3] = ++packetId;
        buffer = session.getFront().writeToBuffer(row, allocBuffer());
    }

    @Override
    public void eof(byte[] eof, BackendConnection con) {
        record(con);
        session.release(con);
        ServerConnection front = session.getFront();
        eof[3] = ++packetId;
        buffer = front.writeToBuffer(eof, allocBuffer());
        front.write(buffer);
    }

    private void record(BackendConnection con) {
        long lastActiveTime = con.getVariables().getLastActiveTime();
        Datasource source = ((MySQLConnection) con).getDatasource().getConfig();
        TimoServer.getSender()
                .send(new Mail<SQLRecord>(TimoServer.getRecorder(),
                        new SQLRecord(source.getHost(), source.getDB(), sql, lastActiveTime,
                                TimeUtil.currentTimeMillis() - lastActiveTime,
                                source.getDatanodeID())));
    }

    @Override
    public void setSQL(String sql) {
        this.sql = sql;
    }

}
