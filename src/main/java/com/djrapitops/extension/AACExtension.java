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
package com.djrapitops.extension;

import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.annotation.NumberProvider;
import com.djrapitops.plan.extension.annotation.PluginInfo;
import com.djrapitops.plan.extension.annotation.TableProvider;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Family;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plan.extension.table.Table;

import java.text.SimpleDateFormat;
import java.util.UUID;

/**
 * AdvancedAntiCheat DataExtension.
 *
 * @author Rsl1122
 */
@PluginInfo(name = "AdvancedAntiCheat", iconName = "heart", iconFamily = Family.SOLID, color = Color.RED)
public class AACExtension implements DataExtension {

    private AACStorage storage;

    public AACExtension() {
        storage = new AACStorage();
        new AACHackKickListener(storage).register();
    }

    AACExtension(boolean forTesting) {
    }

    @Override
    public CallEvents[] callExtensionMethodsOn() {
        return new CallEvents[]{
                CallEvents.PLAYER_LEAVE
        };
    }

    @NumberProvider(
            text = "Kicked for Hacking",
            description = "Times Kicked for Possible Hacking, also includes false positives",
            iconName = "exclamation-triangle",
            iconColor = Color.RED,
            showInPlayerTable = true
    )
    public long timesKickedForHack(UUID playerUUID) {
        return storage.getHackKickCount(playerUUID);
    }

    @TableProvider(tableColor = Color.RED)
    public Table hackKickTable(UUID playerUUID) {
        Table.Factory table = Table.builder()
                .columnOne("Kicked", Icon.called("calendar").of(Family.REGULAR).build())
                .columnTwo("Hack", Icon.called("exclamation-triangle").build())
                .columnThree("Violation Level", Icon.called("gavel").build());

        SimpleDateFormat format = new SimpleDateFormat("MMM d YYYY, HH:mm");
        for (AACHackInfo hack : storage.getHackInformation(playerUUID)) {
            table.addRow(
                    format.format(hack.getDate()),
                    hack.getHackType(),
                    hack.getViolationLevel()
            );
        }

        return table.build();
    }

}