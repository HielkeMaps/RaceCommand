package io.github.hielkemaps.racecommand;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.*;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.RaceManager;
import io.github.hielkemaps.racecommand.race.RaceMode;
import io.github.hielkemaps.racecommand.race.RacePlayer;
import io.github.hielkemaps.racecommand.race.types.InfectedRace;
import io.github.hielkemaps.racecommand.race.types.NormalRace;
import io.github.hielkemaps.racecommand.race.types.PvpRace;
import io.github.hielkemaps.racecommand.wrapper.PlayerManager;
import io.github.hielkemaps.racecommand.wrapper.PlayerWrapper;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.function.Predicate;

public class Commands {

    public Commands() {
        List<Argument> arguments;

        //CREATE NORMAL
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("create").withRequirement(playerInRace.negate()));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    createRace(RaceMode.normal, p);
                }).register();

        //CREATE
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("create").withRequirement(playerInRace.negate()));
        arguments.add(new MultiLiteralArgument(RaceMode.getNames(RaceMode.class)));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    RaceMode mode = RaceMode.valueOf((String) args[0]);
                    createRace(mode, p);
                }).register();

        //START
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("start").withRequirement(playerInRace.and(playerIsRaceOwner).and(raceHasStarted.negate().and(raceIsStarting.negate()))));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {

                    Race race = RaceManager.getRace(p.getUniqueId());
                    if (race == null) return;

                    race.start();
                }).register();

        //START COUNTDOWN
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("start").withRequirement(playerInRace.and(playerIsRaceOwner).and(raceHasStarted.negate().and(raceIsStarting.negate()))));
        arguments.add(new IntegerArgument("countdown", 3, 1000));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    int value = (int) args[0];

                    Race race = RaceManager.getRace(p.getUniqueId());
                    if (race == null) return;

                    race.setCountDown(value);
                    race.start();
                }).register();

        //STOP
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("stop").withRequirement(playerInRace.and(playerIsRaceOwner).and(raceHasStarted.or(raceIsStarting))));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {

                    Race race = RaceManager.getRace(p.getUniqueId());
                    if (race == null) return;
                    race.stop();
                }).register();

        //INVITE
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("invite").withRequirement(playerInRace.and(playerIsRaceOwner)));
        arguments.add(new PlayerArgument("player").withRequirement(sender -> !sender.isOp()).overrideSuggestions(sender -> {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            List<String> names = new ArrayList<>();

            Race race = RaceManager.getRace(((Player) sender).getUniqueId());
            if (race == null) return new String[0];

            //Don't show players that are already in your race
            for (Player p : players) {
                if (race.hasPlayer(p.getUniqueId())) continue;
                names.add(p.getName());
            }
            return names.toArray(new String[0]);
        }));

        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    Player invited = (Player) args[0];

                    //If invite yourself
                    if (invited.getUniqueId().equals(p.getUniqueId())) {
                        p.sendMessage(Main.PREFIX + "You can't invite yourself");
                        return;
                    }

                    Race race = RaceManager.getRace(p.getUniqueId());
                    if (race == null) return;

                    //If player is already in race
                    if (race.hasPlayer(invited.getUniqueId())) {
                        p.sendMessage(Main.PREFIX + "That player is already in your race");
                        return;
                    }

                    //If already invited
                    if (race.hasInvited(invited)) {
                        p.sendMessage(Main.PREFIX + "You have already invited " + invited.getName());
                        return;
                    }

                    race.invitePlayer(invited.getUniqueId());
                    TextComponent msg = new TextComponent(Main.PREFIX + p.getName() + " wants to race! ");
                    TextComponent accept = new TextComponent(ChatColor.GREEN + "[Accept]");
                    accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/race join " + p.getName()));
                    msg.addExtra(accept);

                    Objects.requireNonNull(Bukkit.getPlayer(invited.getUniqueId())).spigot().sendMessage(msg);
                    p.sendMessage(Main.PREFIX + "Invited player " + invited.getName());

                }).register();


        //INVITE ALL - OP ONLY
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("invite").withRequirement(playerInRace.and(playerIsRaceOwner)));
        arguments.add(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS).withPermission(CommandPermission.OP).overrideSuggestions(sender -> {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            List<String> names = new ArrayList<>();

            Race race = RaceManager.getRace(((Player) sender).getUniqueId());
            if (race == null) return new String[0];

            //Don't show players that are already in your race
            for (Player p : players) {
                if (race.hasPlayer(p.getUniqueId())) continue;
                names.add(p.getName());
            }
            return names.toArray(new String[0]);
        }));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    @SuppressWarnings("unchecked")
                    Collection<Player> invitedPlayers = (Collection<Player>) args[0];
                    boolean onePlayerInvited = invitedPlayers.size() == 1;

                    for (Player invited : invitedPlayers) {

                        //If invite yourself
                        if (invited.getUniqueId().equals(p.getUniqueId())) {
                            if (onePlayerInvited) p.sendMessage(Main.PREFIX + "You can't invite yourself");
                            continue;
                        }

                        Race race = RaceManager.getRace(p.getUniqueId());
                        if (race == null) return;

                        //If player is already in your race
                        if (race.hasPlayer(invited.getUniqueId())) {
                            if (onePlayerInvited) p.sendMessage(Main.PREFIX + "That player is already in your race");
                            continue;
                        }

                        //OPs invitation will always go through, even if already invited

                        race.invitePlayer(invited.getUniqueId());
                        TextComponent msg = new TextComponent(Main.PREFIX + p.getName() + " wants to race! ");
                        TextComponent accept = new TextComponent(ChatColor.GREEN + "[Accept]");
                        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/race join " + p.getName()));
                        msg.addExtra(accept);

                        Objects.requireNonNull(Bukkit.getPlayer(invited.getUniqueId())).spigot().sendMessage(msg);
                        p.sendMessage(Main.PREFIX + "Invited player " + invited.getName());
                    }
                }).register();

        //JOIN
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("join").withRequirement(playerHasJoinableRaces));
        arguments.add(new StringArgument("player").overrideSuggestions((sender) -> PlayerManager.getPlayer(((Player) sender).getUniqueId()).getJoinableRaces()));
        new CommandAPICommand("race").withArguments(arguments).executesPlayer((p, args) -> {
            String playerName = (String) args[0];
            UUID uuid = Bukkit.getOfflinePlayer(playerName).getUniqueId();

            Race race = RaceManager.getRace(uuid);
            if (race == null) {
                CommandAPI.fail("Race not found");
                return;
            }

            //If joining own race
            if (race.hasPlayer(p.getUniqueId())) {
                p.sendMessage(Main.PREFIX + "You already joined " + playerName + "'s race");
                return;
            }

            PlayerWrapper wPlayer = PlayerManager.getPlayer(p.getUniqueId());

            if (race.isPublic() || race.hasInvited(p)) {

                if (race.hasStarted()) {
                    p.sendMessage(Main.PREFIX + "Can't join race: This race has already started");
                    return;
                }

                //If player in existing race
                if (wPlayer.isInRace()) {
                    Race raceToLeave = RaceManager.getRace(p.getUniqueId());

                    if (raceToLeave != null) {
                        UUID raceOwner = raceToLeave.getOwner();

                        if (raceOwner.equals(p.getUniqueId())) {
                            RaceManager.disbandRace(p.getUniqueId());
                            p.sendMessage(Main.PREFIX + "You have disbanded the race");
                        } else {
                            raceToLeave.leavePlayer(p.getUniqueId());
                            p.sendMessage(Main.PREFIX + "You have left " + raceToLeave.getName() + "'s race");
                        }
                    }
                }

                //Join race
                if (wPlayer.acceptInvite(uuid)) {
                    p.sendMessage(Main.PREFIX + "You joined " + playerName + "'s race!");
                } else {
                    CommandAPI.fail("Could not join race");
                }
            }
        }).register();

        //DISBAND
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("disband").withRequirement(playerInRace.and(playerIsRaceOwner)));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    RaceManager.disbandRace(p.getUniqueId());
                    p.sendMessage(Main.PREFIX + "You have disbanded the race");
                }).register();

        //LEAVE
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("leave").withRequirement(playerInRace.and(playerIsRaceOwner.negate())));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    Race race = RaceManager.getRace(p.getUniqueId());
                    if (race != null) {
                        race.leavePlayer(p.getUniqueId());
                        p.sendMessage(Main.PREFIX + "You have left the race");
                    }
                }).register();

        //INFO
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("info").withRequirement(playerInRace));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {

                    Race race = RaceManager.getRace(p.getUniqueId());
                    if (race == null) return;

                    String ownerName = race.getName();

                    p.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "           " + ChatColor.RESET + "" + ChatColor.BOLD + " " + ownerName + "'s race " + ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + "           ");
                    p.sendMessage("Visibility: " + (race.isPublic() ? ChatColor.GREEN + "public" : ChatColor.RED + "private"));
                    p.sendMessage("Players:");

                    for (RacePlayer racePlayer : race.getPlayers()) {
                        String name = racePlayer.getName();

                        StringBuilder str = new StringBuilder();
                        str.append(ChatColor.GRAY).append("-");

                        //Gold name if finished
                        if (racePlayer.isFinished()) {
                            str.append(ChatColor.GOLD);
                        }

                        str.append(name);

                        //Display time if finished
                        if (racePlayer.isFinished()) {
                            str.append(ChatColor.WHITE).append(" (");
                            str.append(Util.getTimeString(racePlayer.getTime()));
                            str.append(")");
                        }

                        if (racePlayer.isOwner()) {
                            str.append(ChatColor.GREEN).append(" [Owner]");
                        }

                        if(racePlayer.isInfected()){
                            str.append(ChatColor.GREEN).append(" [Infected]");
                        }

                        if(racePlayer.isSkeleton()){
                            str.append(ChatColor.WHITE).append(" [Skeleton]");
                        }

                        p.sendMessage(str.toString());
                    }
                    p.sendMessage(ChatColor.GOLD + "" + ChatColor.STRIKETHROUGH + ownerName.replaceAll(".", "  ") + "                                ");
                }).register();

        //KICK
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("kick").withRequirement(playerInRace.and(playerIsRaceOwner).and(playerToKick)));
        arguments.add(new StringArgument("player").overrideSuggestions((sender) ->
        {
            List<String> names = new ArrayList<>();
            Race race = RaceManager.getRace(((Player) sender).getUniqueId());
            if (race == null) return new String[0];

            List<RacePlayer> players = race.getPlayers();
            for (RacePlayer racePlayer : players) {
                UUID uuid = racePlayer.getUniqueId();
                if (uuid.equals(((Player) sender).getUniqueId())) continue;
                names.add(Bukkit.getOfflinePlayer(uuid).getName());
            }

            return names.toArray(new String[0]);
        }));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    String playerName = (String) args[0];

                    UUID toKick = Bukkit.getOfflinePlayer(playerName).getUniqueId();

                    if (p.getUniqueId().equals(toKick)) {
                        p.sendMessage(Main.PREFIX + "You can't kick yourself");
                        return;
                    }
                    Race race = RaceManager.getRace(p.getUniqueId());
                    race.kickPlayer(toKick);
                }).register();

        //Option visibility
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("option").withRequirement(playerInRace.and(playerIsRaceOwner)));
        arguments.add(new LiteralArgument("visibility"));
        arguments.add(new MultiLiteralArgument("public", "private"));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    String s = (String) args[0];
                    if (Objects.requireNonNull(RaceManager.getRace(p.getUniqueId())).setIsPublic(s.equals("public"))) {
                        p.sendMessage(Main.PREFIX + "Set race visibility to " + s);
                    } else {
                        p.sendMessage(Main.PREFIX + ChatColor.RED + "Nothing changed. Race visibility was already " + s);
                    }
                }).register();

        //Option ghost players
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("option").withRequirement(playerInRace.and(playerIsRaceOwner)));
        arguments.add(new LiteralArgument("ghostPlayers").withRequirement(playerInInfectedRace.negate())); //not for infected gamemode
        arguments.add(new BooleanArgument("value"));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    boolean value = (boolean) args[0];
                    if (Objects.requireNonNull(RaceManager.getRace(p.getUniqueId())).setGhostPlayers(value)) {

                        if (value) {
                            p.sendMessage(Main.PREFIX + "Players in race will now be see-through");
                        } else {
                            p.sendMessage(Main.PREFIX + "Players in race will no longer be see-through");
                        }
                    } else {
                        p.sendMessage(Main.PREFIX + ChatColor.RED + "Nothing changed. ghostPlayers was already set to " + value);
                    }
                }).register();


        //Option infected player
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("option").withRequirement(playerInRace.and(playerIsRaceOwner)));
        arguments.add(new LiteralArgument("firstInfected").withRequirement(playerInInfectedRace));
        arguments.add(new PlayerArgument("player").overrideSuggestions(sender -> {
            Collection<? extends Player> players = Bukkit.getOnlinePlayers();
            List<String> names = new ArrayList<>();

            Race race = RaceManager.getRace(((Player) sender).getUniqueId());
            if (race == null) return new String[0];

            //Only show players that are in your race
            for (Player p : players) {
                if (race.hasPlayer(p.getUniqueId())) {
                    names.add(p.getName());
                }
            }
            return names.toArray(new String[0]);
        }));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    Player player = (Player) args[0];
                    Race race = RaceManager.getRace(p.getUniqueId());
                    if (race instanceof InfectedRace) {
                        RacePlayer racePlayer = race.getRacePlayer(player.getUniqueId());
                        if (racePlayer != null) {
                            ((InfectedRace) race).setFirstInfected(racePlayer);
                            p.sendMessage(Main.PREFIX + player.getName() + " will be the first infected");
                        } else {
                            CommandAPI.fail("Could not find player " + player.getName() + ".");
                        }
                    }
                }).register();

        //Option infected player
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("option").withRequirement(playerInRace.and(playerIsRaceOwner)));
        arguments.add(new LiteralArgument("infectedDelay").withRequirement(playerInInfectedRace));
        arguments.add(new IntegerArgument("seconds", 5));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    int delay = (Integer) args[0];
                    Race race = RaceManager.getRace(p.getUniqueId());
                    if (race instanceof InfectedRace) {
                        ((InfectedRace) race).setInfectedDelay(delay);
                        race.sendMessage(Main.PREFIX + "Infected delay set to " + delay + " seconds");
                    }
                }).register();

        //Option broadcast - OP ONLY
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("option").withRequirement(playerInRace.and(playerIsRaceOwner)));
        arguments.add(new LiteralArgument("broadcast").withPermission(CommandPermission.OP));
        arguments.add(new BooleanArgument("value"));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {
                    boolean value = (boolean) args[0];

                    Race race = RaceManager.getRace(p.getUniqueId());
                    if (race == null) return;

                    if (race.setBroadcast(value)) {
                        if (value) {
                            p.sendMessage(Main.PREFIX + "Enabled broadcasting");
                        } else {
                            p.sendMessage(Main.PREFIX + "Disabled broadcasting");
                        }

                    } else {
                        p.sendMessage(Main.PREFIX + ChatColor.RED + "Nothing changed. Broadcast was already set to " + value);
                    }
                }).register();

        //Force join - OP ONLY
        arguments = new ArrayList<>();
        arguments.add(new LiteralArgument("forcejoin").withRequirement(playerInRace.and(playerIsRaceOwner)).withPermission(CommandPermission.OP));
        arguments.add(new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS));
        new CommandAPICommand("race")
                .withArguments(arguments)
                .executesPlayer((p, args) -> {

                    @SuppressWarnings("unchecked")
                    Collection<Player> players = (Collection<Player>) args[0];
                    boolean onePlayerJoins = players.size() == 1;

                    Race newRace = RaceManager.getRace(p.getUniqueId());
                    if (newRace == null) return;

                    for (Player player : players) {

                        // You cant join your own race
                        if (player.getUniqueId().equals(p.getUniqueId())) continue;

                        PlayerWrapper wPlayer = PlayerManager.getPlayer(player.getUniqueId());

                        //leave old race
                        if (wPlayer.isInRace()) {
                            Race race = RaceManager.getRace(player.getUniqueId());
                            if (race == null) continue;

                            //If player is race owner, disband race
                            //Otherwise leave race
                            if (race.isOwner(player.getUniqueId())) {
                                RaceManager.disbandRace(player.getUniqueId());
                                player.sendMessage(Main.PREFIX + "You have disbanded the race");
                            } else {
                                //If already in the same race, do nothing
                                if (newRace.getOwner().equals(race.getOwner())) {
                                    if (onePlayerJoins)
                                        p.sendMessage(Main.PREFIX + player.getName() + " is already in the race");
                                    continue;
                                }
                                race.leavePlayer(player.getUniqueId());
                                player.sendMessage(Main.PREFIX + "You have left the race");
                            }
                        }

                        newRace.addPlayer(player.getUniqueId());

                        if (newRace.isStarting()) {
                            player.performCommand("restart");
                        }

                        //allow new player even if race has started
                        if (newRace.hasStarted()) {
                            player.performCommand("restart");

                            //+2 ticks
                            Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getInstance(), () -> {
                                newRace.executeStartFunction(player); //run start function 2 ticks later so restart was successful
                                player.addScoreboardTag("inRace");

                                //+2 ticks
                                Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getInstance(), () -> {
                                    newRace.syncTime(player);
                                    player.teleport(p);
                                }, 2L);
                            }, 2L);
                        }

                        player.sendMessage(Main.PREFIX + "You joined " + p.getName() + "'s race!");
                    }
                }).register();
    }

    Predicate<CommandSender> playerInRace = sender -> PlayerManager.getPlayer(((Player) sender).getUniqueId()).isInRace();

    Predicate<CommandSender> playerHasJoinableRaces = sender -> PlayerManager.getPlayer(((Player) sender).getUniqueId()).hasJoinableRace();

    Predicate<CommandSender> playerInInfectedRace = sender -> PlayerManager.getPlayer(((Player) sender).getUniqueId()).isInInfectedRace();

    Predicate<CommandSender> playerIsRaceOwner = sender -> {
        Player player = (Player) sender;
        if (player == null) return false;

        Race race = RaceManager.getRace(player.getUniqueId());
        if (race == null) return false;

        return race.isOwner(player.getUniqueId());
    };

    Predicate<CommandSender> raceHasStarted = sender -> {
        Player player = (Player) sender;
        if (player == null) return false;

        Race race = RaceManager.getRace(player.getUniqueId());
        if (race == null) return false;

        return race.hasStarted();
    };

    Predicate<CommandSender> raceIsStarting = sender -> {
        Player player = (Player) sender;
        if (player == null) return false;

        Race race = RaceManager.getRace(player.getUniqueId());
        if (race == null) return false;

        return race.isStarting();
    };

    Predicate<CommandSender> playerToKick = sender -> {
        Player player = (Player) sender;
        if (player == null) return false;

        Race race = RaceManager.getRace(player.getUniqueId());
        if (race == null) return false;

        return race.getPlayers().size() > 1;
    };

    public void createRace(RaceMode mode, Player p) {
        Race race;
        switch (mode) {
            case pvp:
                race = new PvpRace(p.getUniqueId(), p.getName());
                break;
            case infected:
                race = new InfectedRace(p.getUniqueId(), p.getName());
                break;
            default:
                race = new NormalRace(p.getUniqueId(), p.getName());
        }
        RaceManager.addRace(race);

        TextComponent msg = new TextComponent(Main.PREFIX + "Created race! Invite players with ");
        TextComponent click = new TextComponent(ChatColor.WHITE + "/race invite");
        click.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/race invite "));
        msg.addExtra(click);

        p.spigot().sendMessage(msg);
    }
}
