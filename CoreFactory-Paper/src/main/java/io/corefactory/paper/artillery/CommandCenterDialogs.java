package io.corefactory.paper.artillery;

import io.corefactory.core.artillery.ArtilleryMagazine;
import io.corefactory.core.artillery.ArtilleryShell;
import io.corefactory.core.artillery.ChargeTier;
import io.corefactory.core.text.Text;
import io.corefactory.paper.energy.EnergyLevels;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.action.DialogActionCallback;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Dialogs of a Command Center: the main menu, the list of connected artillery, each piece's own dialog and its info, and the Shell
 * dialog. Each button is a custom click action, so the click runs code rather than a command. A dialog is built for one player, who is the
 * owner of the center or a friend of the owner, and who is the only one who sees it; a button whose piece or center has gone does nothing but
 * say so. The Load takes the shell and the charge from that player's inventory.
 */
public final class CommandCenterDialogs {

    private static final ClickCallback.Options OPTIONS = ClickCallback.Options.builder()
            .uses(ClickCallback.UNLIMITED_USES)
            .lifetime(ClickCallback.DEFAULT_LIFETIME)
            .build();

    private static final int BUTTON_WIDTH = 200;
    private static final int COLUMNS = 2;

    private final CommandCenterService service;
    private final TargetingMode targeting;

    /**
     * @param service   the Command Centers' runtime
     * @param targeting the Set Target mode, which the main menu starts
     */
    public CommandCenterDialogs(CommandCenterService service, TargetingMode targeting) {
        this.service = service;
        this.targeting = targeting;
    }

    /** Opens the main menu of a center for the player who may use it: its owner or a friend of the owner. */
    public void main(Player player, CommandCenter center) {
        player.showDialog(mainDialog(player, center));
    }

    private Dialog mainDialog(Player player, CommandCenter center) {
        List<ActionButton> buttons = new ArrayList<>();
        buttons.add(button(Text.parse("View Artillery Machines"), null, (view, audience) -> {
            if (live(center)) {
                machines(player, center, 0);
            }
        }));
        buttons.add(button(Text.parse("Set Target"), null, (view, audience) -> {
            if (live(center)) {
                targeting.begin(player, center);
            }
        }));
        if (center.hasAim()) {
            buttons.add(button(Text.parse("Clear target"), null, (view, audience) -> {
                if (live(center)) {
                    service.clearStrike(center);
                    player.showDialog(mainDialog(player, center));
                }
            }));
            buttons.add(button(Text.parse("Launch"), null, (view, audience) -> {
                if (live(center)) {
                    service.launch(player, center);
                    player.showDialog(mainDialog(player, center));
                }
            }));
        }
        buttons.add(button(Text.parse("Perform Safety Check"), null, (view, audience) -> {
            if (live(center)) {
                service.checkFor(center, player);
                player.showDialog(mainDialog(player, center));
            }
        }));
        buttons.add(button(Text.parse("Choose Shell"), null, (view, audience) -> {
            if (live(center)) {
                shells(player, center);
            }
        }));
        return dialog(Text.parse("<accent>Artillery Command Center</accent>"), body(readoutText(center)), List.of(),
                multiAction(buttons, closeButton()));
    }

    /** The list of the connected artillery, one page at a time. */
    private void machines(Player player, CommandCenter center, int requested) {
        List<ArtilleryDevice> connected = service.connected(center);
        int page = MachinePages.clamp(requested, connected.size());
        List<ActionButton> buttons = new ArrayList<>();
        for (ArtilleryDevice piece : MachinePages.slice(connected, page)) {
            buttons.add(button(machineLabel(piece), null, (view, audience) -> {
                if (live(center) && livePiece(piece)) {
                    machine(player, center, piece, page);
                }
            }));
        }
        if (MachinePages.hasNext(page, connected.size())) {
            buttons.add(button(Text.parse("Next page"), null, (view, audience) -> machines(player, center, page + 1)));
        }
        if (MachinePages.hasBack(page)) {
            buttons.add(button(Text.parse("Back page"), null, (view, audience) -> machines(player, center, page - 1)));
        }
        buttons.add(button(Text.parse("Back"), null, (view, audience) -> {
            if (live(center)) {
                main(player, center);
            }
        }));
        String heading = connected.isEmpty() ? "<muted>No artillery is connected to this command center.</muted>"
                : "<muted>" + connected.size() + " connected, page " + (page + 1) + " of " + MachinePages.count(connected.size())
                        + ".</muted>";
        player.showDialog(dialog(Text.parse("<accent>Artillery Machines</accent>"), body(Text.parse(heading)), List.of(),
                multiAction(buttons, closeButton())));
    }

    /**
     * One piece's dialog: its name, its switch, its magazine with a Load and an Unload, its info and a way back to the list. A Load takes one
     * shell and one charge, of the shell and the tier the center is set to, out of the inventory of the player who clicks into the magazine.
     */
    private void machine(Player player, CommandCenter center, ArtilleryDevice piece, int page) {
        List<ActionButton> buttons = new ArrayList<>();
        String switchLabel = piece.on() ? "Turn OFF" : "Turn ON";
        buttons.add(button(Text.parse(piece.on() ? "<bad>" + switchLabel + "</bad>" : "<good>" + switchLabel + "</good>"), null,
                (view, audience) -> {
                    if (live(center) && livePiece(piece)) {
                        rename(view, piece);
                        service.artillery().setOn(piece, !piece.on());
                        machine(player, center, piece, page);
                    }
                }));
        buttons.add(button(Text.parse("Load"), null, (view, audience) -> {
            if (live(center) && livePiece(piece)) {
                rename(view, piece);
                load(player, center, piece);
                machine(player, center, piece, page);
            }
        }));
        buttons.add(button(Text.parse("Unload"), null, (view, audience) -> {
            if (live(center) && livePiece(piece)) {
                rename(view, piece);
                player.sendMessage(Text.parse(service.artillery().unload(piece, player).message()));
                machine(player, center, piece, page);
            }
        }));
        buttons.add(button(Text.parse("Info"), null, (view, audience) -> {
            if (live(center) && livePiece(piece)) {
                info(player, center, piece, page);
            }
        }));
        buttons.add(button(Text.parse("Back"), null, (view, audience) -> {
            if (live(center) && livePiece(piece)) {
                rename(view, piece);
                machines(player, center, page);
            }
        }));
        List<DialogInput> inputs = List.of(DialogInput.text("name", Text.parse("Name"))
                .width(BUTTON_WIDTH)
                .labelVisible(true)
                .initial(piece.name())
                .maxLength(ArtilleryDevice.MAX_NAME_LENGTH)
                .build());
        Component header = Component.join(JoinConfiguration.separator(Component.newline()),
                Text.parse("<value>" + Text.escape(piece.name()) + "</value> <muted>level "
                        + EnergyLevels.roman(piece.level().number()) + ", " + (piece.on() ? "<good>on</good>" : "<bad>off</bad>")
                        + "</muted>"),
                magazineLine(piece),
                Text.parse("<muted>Load takes: </muted><value>" + ShellMenu.selection(center.shell(), center.tier()) + "</value>"));
        player.showDialog(dialog(Text.parse("<accent>Artillery Machine</accent>"), body(header), inputs,
                multiAction(buttons, closeButton())));
    }

    /** The magazine of a piece as a line: its shells and charges, and the most of each a load brings. */
    private Component magazineLine(ArtilleryDevice piece) {
        ArtilleryMagazine magazine = service.artillery().magazine(piece);
        return Text.parse("<muted>Magazine:</muted> <value>" + magazine.shells() + " shells, " + magazine.charges()
                + " charges</value> <muted>of " + magazine.capacity() + " each at most</muted>");
    }

    /**
     * Loads one shell and one charge of the center's choice into a piece, and says what happened. A center with no shell chosen says so.
     */
    private void load(Player player, CommandCenter center, ArtilleryDevice piece) {
        if (center.shell() == null) {
            player.sendMessage(Text.parse("&cChoose a shell first: Choose Shell on the command center."));
            return;
        }
        ArtilleryService.LoadResult result = service.artillery().load(piece, center.shell(), center.tier(), player);
        player.sendMessage(Text.parse(result.message()));
    }

    /** The info of a piece: its wattage, the shell and tier it carries, and its target and elevation. */
    private void info(Player player, CommandCenter center, ArtilleryDevice piece, int page) {
        String shell = piece.shell() == null ? "<hint>none</hint>"
                : "<value>" + ShellMenu.shellName(piece.shell()) + "</value>";
        String target = piece.hasAim()
                ? String.format(Locale.ROOT, "X %d, Z %d, elevation %.0f deg", Math.round(piece.aimX()),
                Math.round(piece.aimZ()), piece.elevation())
                : "none";
        List<Component> lines = List.of(
                Text.parse("Wattage <value>" + CommandCenterScreen.power(service.artillery().settings().powerOf(piece.level()))
                        + "</value> at full power"),
                Text.parse("Shell " + shell + " <muted>tier " + ShellMenu.tierNumeral(piece.tier()) + "</muted>"),
                magazineLine(piece),
                Text.parse("Target <value>" + target + "</value>"));
        ActionButton back = button(Text.parse("Back"), null, (view, audience) -> {
            if (live(center) && livePiece(piece)) {
                machine(player, center, piece, page);
            }
        });
        player.showDialog(dialog(Text.parse("<accent>Artillery Info</accent>"),
                body(Component.join(JoinConfiguration.separator(Component.newline()), lines)), List.of(), DialogType.notice(back)));
    }

    /** The Shell dialog: nine shells and three charge tiers, with the warnings of the connected artillery. */
    private void shells(Player player, CommandCenter center) {
        List<ArtilleryDevice> connected = service.connected(center);
        int highest = CommandCenterService.highestLevel(connected);
        List<ActionButton> buttons = new ArrayList<>();
        for (ShellMenu.ShellRow row : ShellMenu.shells(highest)) {
            String label = ShellMenu.label(row.name(), row.shell() == center.shell(), row.warn());
            Component tooltip = row.warn() ? Text.parse("<warn>" + row.reason() + "</warn>") : null;
            ArtilleryShell shell = row.shell();
            buttons.add(button(Text.parse(label), tooltip, (view, audience) -> {
                if (live(center)) {
                    service.setLoad(center, shell, center.tier());
                    shells(player, center);
                }
            }));
        }
        for (ShellMenu.TierRow row : ShellMenu.tiers(CommandCenterService.loadedTiers(connected))) {
            String label = ShellMenu.label(row.name(), row.tier() == center.tier(), row.warn());
            Component tooltip = row.warn() ? Text.parse("<warn>" + row.reason() + "</warn>") : null;
            ChargeTier tier = row.tier();
            buttons.add(button(Text.parse(label), tooltip, (view, audience) -> {
                if (live(center)) {
                    service.setLoad(center, center.shell(), tier);
                    shells(player, center);
                }
            }));
        }
        Component heading = Component.join(JoinConfiguration.separator(Component.newline()),
                Text.parse("<muted>Selected: </muted><value>" + ShellMenu.selection(center.shell(), center.tier()) + "</value>"),
                Text.parse("<muted>" + (connected.isEmpty() ? "No artillery is connected." : "Highest connected level "
                        + EnergyLevels.roman(highest) + ".") + "</muted>"));
        ActionButton back = button(Text.parse("Back"), null, (view, audience) -> {
            if (live(center)) {
                main(player, center);
            }
        });
        player.showDialog(dialog(Text.parse("<accent>Shell</accent>"), body(heading), List.of(),
                multiAction(buttons, back)));
    }

    /** The main menu's notice: the same lines the screen shows. */
    private Component readoutText(CommandCenter center) {
        List<Component> lines = CommandCenterScreen.lines(service.readout(center)).stream().map(Text::parse).toList();
        return Component.join(JoinConfiguration.separator(Component.newline()), lines);
    }

    /** Saves the name typed into a piece's name field. A blank field keeps the name. */
    private void rename(DialogResponseView view, ArtilleryDevice piece) {
        String typed = view == null ? null : view.getText("name");
        if (typed != null) {
            service.artillery().rename(piece, typed);
        }
    }

    private boolean live(CommandCenter center) {
        return service.centers().byId(center.id()).isPresent();
    }

    private boolean livePiece(ArtilleryDevice piece) {
        return service.artillery().registry().byId(piece.id()).isPresent();
    }

    private static Component machineLabel(ArtilleryDevice piece) {
        return Text.parse("<value>" + Text.escape(piece.name()) + "</value> <muted>" + EnergyLevels.roman(piece.level().number())
                + " at " + piece.pos().x() + ", " + piece.pos().y() + ", " + piece.pos().z() + "</muted>");
    }

    private static ActionButton button(Component label, Component tooltip, DialogActionCallback callback) {
        ActionButton.Builder builder = ActionButton.builder(label).width(BUTTON_WIDTH)
                .action(DialogAction.customClick(callback, OPTIONS));
        if (tooltip != null) {
            builder = builder.tooltip(tooltip);
        }
        return builder.build();
    }

    private static ActionButton closeButton() {
        return button(Text.parse("Close"), null, (view, audience) -> {
        });
    }

    private static DialogType multiAction(List<ActionButton> buttons, ActionButton exit) {
        return DialogType.multiAction(buttons).exitAction(exit).columns(COLUMNS).build();
    }

    private static List<DialogBody> body(Component text) {
        return List.of(DialogBody.plainMessage(text));
    }

    private static Dialog dialog(Component title, List<DialogBody> body, List<DialogInput> inputs, DialogType type) {
        return Dialog.create(factory -> factory.empty()
                .base(DialogBase.builder(title).canCloseWithEscape(true).body(body).inputs(inputs).build())
                .type(type));
    }
}
