package io.github.hielkemaps.racecommand.race.types;

import io.github.hielkemaps.racecommand.Main;
import io.github.hielkemaps.racecommand.race.Race;
import io.github.hielkemaps.racecommand.race.player.RacePlayer;
import io.github.hielkemaps.racecommand.race.player.types.DefaultRacePlayer;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.util.UUID;

public class NormalRace extends Race {

    public NormalRace(UUID uniqueId, String name) {
        super(uniqueId,name);
    }

    @Override
    protected void onPlayerStart(RacePlayer racePlayer) {

    }

    @Override
    protected void onRaceStop() {
        //show results if any, otherwise show stop message
        if (players.stream().noneMatch(RacePlayer::isFinished)) sendMessage(Main.PREFIX + "Stopped race");
        else printResults();
    }

    @Override
    public void onTick() {

    }

    @Override
    public void onPlayerDamagedByPlayer(EntityDamageByEntityEvent e, RacePlayer target, RacePlayer attacker) {
        e.setCancelled(true);
    }

    @Override
    public void onPlayerDamagedByEntity(EntityDamageByEntityEvent e, RacePlayer player) {

    }

    @Override
    public void onPlayerQuit(PlayerQuitEvent e, RacePlayer racePlayer) {

    }

    @Override
    public void onPlayerHeal(EntityRegainHealthEvent e, RacePlayer racePlayer) {

    }

    @Override
    public void onPlayerJoin(PlayerJoinEvent e, RacePlayer racePlayer) {

    }

    @Override
    public RacePlayer onPlayerJoin(UUID uuid) {
        return new DefaultRacePlayer(this,uuid);
    }

    @Override
    public void onPlayerMove(PlayerMoveEvent e, RacePlayer racePlayer) {
    }

    @Override
    public void onCancelTasks() {
    }

    @Override
    public void onPlayerRespawn(PlayerRespawnEvent e, RacePlayer racePlayer) {

    }

    @Override
    protected void onPlayerLeave(RacePlayer racePlayer) {

    }

    @Override
    public void onPlayerDeath(PlayerDeathEvent e, RacePlayer racePlayer) {

    }
}
