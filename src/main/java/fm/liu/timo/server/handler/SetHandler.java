/*
 * Copyright 1999-2012 Alibaba Group.
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
package fm.liu.timo.server.handler;

import static fm.liu.timo.server.parser.ServerParseSet.CHARACTER_SET_CLIENT;
import static fm.liu.timo.server.parser.ServerParseSet.CHARACTER_SET_CONNECTION;
import static fm.liu.timo.server.parser.ServerParseSet.CHARACTER_SET_RESULTS;
import static fm.liu.timo.server.parser.ServerParseSet.NAMES;
import org.pmw.tinylog.Logger;
import fm.liu.timo.config.ErrorCode;
import fm.liu.timo.net.mysql.OkPacket;
import fm.liu.timo.server.ServerConnection;
import fm.liu.timo.server.parser.ServerParseSet;
import fm.liu.timo.server.response.CharacterSet;

/**
 * SET 语句处理
 * 
 * @author xianmao.hexm
 */
public final class SetHandler {

    public static void handle(String stmt, ServerConnection c, int offset) {
        int rs = ServerParseSet.parse(stmt, offset);
        switch (rs & 0xff) {
            // case AUTOCOMMIT_ON:
            // if (c.isAutocommit()) {
            // c.write(c.writeToBuffer(OkPacket.OK, c.allocate()));
            // } else {
            // c.commit();
            // c.setAutocommit(true);
            // }
            // break;
            // case AUTOCOMMIT_OFF: {
            // if (c.isAutocommit()) {
            // c.setAutocommit(false);
            // }
            // c.write(c.writeToBuffer(AC_OFF, c.allocate()));
            // break;
            // }
            // case TX_READ_UNCOMMITTED: {
            // c.setTxIsolation(Isolations.READ_UNCOMMITTED);
            // c.write(c.writeToBuffer(OkPacket.OK, c.allocate()));
            // break;
            // }
            // case TX_READ_COMMITTED: {
            // c.setTxIsolation(Isolations.READ_COMMITTED);
            // c.write(c.writeToBuffer(OkPacket.OK, c.allocate()));
            // break;
            // }
            // case TX_REPEATED_READ: {
            // c.setTxIsolation(Isolations.REPEATED_READ);
            // c.write(c.writeToBuffer(OkPacket.OK, c.allocate()));
            // break;
            // }
            // case TX_SERIALIZABLE: {
            // c.setTxIsolation(Isolations.SERIALIZABLE);
            // c.write(c.writeToBuffer(OkPacket.OK, c.allocate()));
            // break;
            // }
            case NAMES:
                String charset = stmt.substring(rs >>> 8).trim();
                if (c.getVariables().setCharset(charset)) {
                    c.write(c.writeToBuffer(OkPacket.OK, c.allocate()));
                } else {
                    c.writeErrMessage(ErrorCode.ER_UNKNOWN_CHARACTER_SET,
                            "Unknown charset '" + charset + "'");
                }
                break;
            case CHARACTER_SET_CLIENT:
            case CHARACTER_SET_CONNECTION:
            case CHARACTER_SET_RESULTS:
                CharacterSet.response(stmt, c, rs);
                break;
            default:
                Logger.warn("SQL:'{}' from {} is not executed", stmt, c);
                c.write(c.writeToBuffer(OkPacket.OK, c.allocate()));
        }
    }

}
