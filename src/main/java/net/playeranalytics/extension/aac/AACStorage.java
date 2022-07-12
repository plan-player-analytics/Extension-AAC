/*
    Copyright(c) 2019 Risto Lahtela (Rsl1122)

    The MIT License(MIT)

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files(the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions :
    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
*/
package net.playeranalytics.extension.aac;

import com.djrapitops.plan.extension.NotReadyException;
import com.djrapitops.plan.query.CommonQueries;
import com.djrapitops.plan.query.QueryService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AACStorage {

    private final QueryService queryService;

    public AACStorage() {
        queryService = QueryService.getInstance();
        createTable();
        queryService.subscribeDataClearEvent(this::recreateTable);
        queryService.subscribeToPlayerRemoveEvent(this::removePlayer);
    }

    private void createTable() {
        String dbType = queryService.getDBType();
        boolean sqlite = dbType.equalsIgnoreCase("SQLITE");

        patchTable(sqlite);

        String sql = "CREATE TABLE IF NOT EXISTS plan_aac_hack_table (" +
                "id int " + (sqlite ? "PRIMARY KEY" : "NOT NULL AUTO_INCREMENT") + ',' +
                "uuid varchar(36) NOT NULL," +
                "server_uuid varchar(36) NOT NULL," +
                "date bigint NOT NULL," +
                "hack_type varchar(100) NOT NULL," +
                "violation_level int NOT NULL" +
                (sqlite ? "" : ",PRIMARY KEY (id)") +
                ')';

        queryService.execute(sql, PreparedStatement::execute);
    }

    private void patchTable(boolean sqlite) {
        CommonQueries commonQueries = queryService.getCommonQueries();
        if (commonQueries.doesDBHaveTable("plan_aac_hack_table")) {
            if (!commonQueries.doesDBHaveTableColumn(
                    "plan_aac_hack_table", "server_uuid"
            )) {
                UUID serverUUID = queryService.getServerUUID().orElseThrow(IllegalStateException::new);
                queryService.execute("ALTER TABLE plan_aac_hack_table ADD " +
                                (sqlite ? "COLUMN " : "") +
                                "server_uuid varchar(36) NOT NULL DEFAULT '" + serverUUID.toString() + "'",
                        PreparedStatement::execute);
            }
            if (!commonQueries.doesDBHaveTableColumn(
                    "plan_aac_hack_table", "date"
            )) {
                queryService.execute("ALTER TABLE plan_aac_hack_table ADD " +
                        (sqlite ? "COLUMN " : "") +
                        "date bigint NOT NULL DEFAULT 0", PreparedStatement::execute);
            }
        }
    }

    private void dropTable() {
        queryService.execute("DROP TABLE IF EXISTS plan_aac_hack_table", PreparedStatement::execute);
    }

    private void recreateTable() {
        dropTable();
        createTable();
    }

    private void removePlayer(UUID playerUUID) {
        queryService.execute(
                "DELETE FROM plan_aac_hack_table WHERE uuid=?",
                statement -> {
                    statement.setString(1, playerUUID.toString());
                    statement.execute();
                }
        );
    }

    public void storeHackKickInformation(UUID playerUUID, AACHackInfo info) {
        String insert = "INSERT INTO plan_aac_hack_table" +
                " (uuid, server_uuid, date, hack_type, violation_level)" +
                " VALUES (?, ?, ?, ?, ?)";

        UUID serverUUID = queryService.getServerUUID().orElseThrow(IllegalStateException::new);
        queryService.execute(insert, statement -> {
            statement.setString(1, info.getUuid().toString());
            statement.setString(2, serverUUID.toString());
            statement.setLong(3, info.getDate());
            statement.setString(4, info.getHackType());
            statement.setInt(5, info.getViolationLevel());

            statement.execute();
        });
    }

    public List<AACHackInfo> getHackInformation(UUID playerUUID) {
        String sql = "SELECT * FROM plan_aac_hack_table" +
                " WHERE uuid=? AND server_uuid=?" +
                " ORDER BY date DESC";

        UUID serverUUID = queryService.getServerUUID().orElseThrow(NotReadyException::new);
        return queryService.query(sql, statement -> {
            statement.setString(1, playerUUID.toString());
            statement.setString(2, serverUUID.toString());
            List<AACHackInfo> hackInfos = new ArrayList<>();

            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    long date = set.getLong("date");
                    String hackType = set.getString("hack_type");
                    int violationLevel = set.getInt("violation_level");
                    hackInfos.add(new AACHackInfo(playerUUID, date, hackType, violationLevel));
                }
                return hackInfos;
            }
        });
    }

    public int getHackKickCount(UUID playerUUID) {
        UUID serverUUID = queryService.getServerUUID()
                .orElseThrow(NotReadyException::new);
        final String sql = "SELECT COUNT(1) as count" +
                " FROM plan_aac_hack_table" +
                " WHERE uuid=?" +
                " AND server_uuid=?";
        return queryService.query(sql, statement -> {
            statement.setString(1, playerUUID.toString());
            statement.setString(2, serverUUID.toString());
            try (ResultSet set = statement.executeQuery()) {
                return set.next() ? set.getInt("count") : 0;
            }
        });
    }
}
