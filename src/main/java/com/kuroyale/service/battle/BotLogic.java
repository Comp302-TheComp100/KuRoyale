package com.kuroyale.service.battle;

import com.kuroyale.model.entities.*;
import com.kuroyale.model.enums.CardType;
import com.kuroyale.model.enums.TargetType;
import com.kuroyale.model.logic.*;
import com.kuroyale.model.state.GameState;
import com.kuroyale.service.battle.ai.CardProfiles;
import com.kuroyale.service.battle.ai.CardRole;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

//Basic AI for the opponent. Waits for full elixir, then places a random affordable unit at the bridge.
public class BotLogic {
    private final ElixirManager elixirManager;
    private final Hand hand;
    private final Random random;
    private double timeSinceLastMove;
    private static final double MOVE_DELAY = 2.0; // Seconds between moves

    // Tracking for combo logic
    private Card lastPlayedCard;
    private double timeSinceLastPlayed; // Seconds
    private static final double COMBO_WINDOW = 5.0;

    private static final double FORCE_PLAY_ELIXIR = 9.5;
    private static final double PASSIVE_ELIXIR_THRESHOLD = 9.0;
    private static final double OFFENSE_ELIXIR_THRESHOLD = 6.0;

    private static final int DEFENSIVE_BUILDING_MIN_Y = com.kuroyale.util.config.GameConstants.RIVER_ROW_1 - 8;
    private static final int DEFENSIVE_BUILDING_MAX_Y = com.kuroyale.util.config.GameConstants.RIVER_ROW_1 - 1;

    public BotLogic(Deck deck) {
        this.elixirManager = new ElixirManager();
        this.hand = new Hand(deck);
        this.random = new Random();
        this.timeSinceLastMove = 0;
        this.timeSinceLastPlayed = 100.0; // Start with no recent play
    }

    // Updates bot state and decides on moves
    public Move update(double deltaTime, GameState gameState) {
        timeSinceLastMove += deltaTime;
        timeSinceLastPlayed += deltaTime;

        // 1. Wait until elixir is high (>= 7) or full
        // 2. Wait for move delay
        // 3. Pick a smart card from hand (prioritizing combos)
        // 4. If affordable, place it

        if (gameState == null) {
            return null;
        }

        double elixir = elixirManager.getCurrentElixir();
        ThreatReport threat = analyzeThreats(extractEnemyTroops(gameState));

        boolean forcePlay = elixir >= FORCE_PLAY_ELIXIR;
        if (!forcePlay && elixir < PASSIVE_ELIXIR_THRESHOLD && threat.threatElixir <= 0) {
            return null;
        }

        if (!forcePlay && timeSinceLastMove < MOVE_DELAY) {
            return null;
        }

        Move move = attemptSmartMove(gameState, threat, forcePlay);
        if (move == null) {
            return null;
        }

        Card cardToPlay = move.card;
        if (cardToPlay == null || !elixirManager.canAfford(cardToPlay.getCost())) {
            return null;
        }

        if (!isValidPlacement(gameState, cardToPlay, move.x, move.y)) {
            return null;
        }

        int playedIndex = findCardIndexInHand(cardToPlay);
        if (playedIndex < 0) {
            return null;
        }

        elixirManager.spend(cardToPlay.getCost());
        hand.playCard(playedIndex);
        timeSinceLastMove = 0;

        lastPlayedCard = cardToPlay;
        timeSinceLastPlayed = 0;

        return move;
    }

    private Move attemptSmartMove(GameState gameState, ThreatReport threat, boolean forcePlay) {
        List<CandidateMove> candidates = new ArrayList<>();

        CandidateMove spell = findBestSpellMove(gameState, threat);
        if (spell != null) {
            candidates.add(spell);
        }

        if (threat != null && threat.threatElixir > 0) {
            CandidateMove defense = findBestDefenseMove(gameState, threat);
            if (defense != null) {
                candidates.add(defense);
            }
        }

        if (threat == null || threat.threatElixir <= 0) {
            CandidateMove offense = findBestOffenseMove(gameState);
            if (offense != null) {
                candidates.add(offense);
            }
        }

        if (candidates.isEmpty() && forcePlay) {
            CandidateMove cycle = findCycleMove(gameState);
            if (cycle != null) {
                candidates.add(cycle);
            }
        }

        CandidateMove best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (CandidateMove c : candidates) {
            if (c == null || c.card == null) {
                continue;
            }
            if (!elixirManager.canAfford(c.card.getCost())) {
                continue;
            }
            if (!isValidPlacement(gameState, c.card, c.x, c.y)) {
                continue;
            }
            if (c.score > bestScore) {
                bestScore = c.score;
                best = c;
            }
        }

        if (best == null) {
            return null;
        }
        return new Move(best.card, best.x, best.y);
    }

    private int findComboCard(java.util.List<Integer> indices) {
        // Priority 1: Siege Mode (Mortar + Defensive Building)
        for (int i : indices) {
            if (checkSiegeMode(hand.getCard(i)))
                return i;
        }

        // Priority 2: Air Assault (Minion + Minion)
        for (int i : indices) {
            if (checkAirAssault(hand.getCard(i)))
                return i;
        }

        // Priority 3: Other Combos (Tank+Support, etc.)
        for (int i : indices) {
            if (checkOtherCombos(hand.getCard(i)))
                return i;
        }

        return -1;
    }

    // --- Combo Check Helpers ---

    private boolean checkSiegeMode(Card current) {
        boolean mortarPlayed = lastPlayedCard.getName().equals("Mortar") || current.getName().equals("Mortar");
        boolean defensePlayed = isDefensiveBuilding(lastPlayedCard) || isDefensiveBuilding(current);
        return mortarPlayed && defensePlayed;
    }

    private boolean checkAirAssault(Card current) {
        return lastPlayedCard.getName().contains("Minion") && current.getName().contains("Minion");
    }

    private boolean checkOtherCombos(Card current) {
        // Tank + Support
        if (isTank(lastPlayedCard) && isRangedTroop(current))
            return true;

        // Swarm
        if (isSwarm(lastPlayedCard) && isSwarm(current))
            return true;

        // Building Defense
        if (lastPlayedCard.getType() == com.kuroyale.model.enums.CardType.BUILDING &&
                current.getType() == com.kuroyale.model.enums.CardType.BUILDING)
            return true;

        // Royal Combo
        boolean knight = lastPlayedCard.getName().equals("Knight") || current.getName().equals("Knight");
        boolean archers = lastPlayedCard.getName().equals("Archers") || current.getName().equals("Archers");
        if (knight && archers)
            return true;

        return false;
    }

    // --- Helpers from ComboService (Duplicated simplified) ---
    private boolean isDefensiveBuilding(Card c) {
        return c.getType() == com.kuroyale.model.enums.CardType.BUILDING &&
                !c.getName().equals("Mortar") && !c.getName().equals("X-Bow");
    }

    private boolean isTank(Card c) {
        return c.getName().equals("Giant") || c.getName().equals("Knight") ||
                c.getName().equals("Golem") || c.getName().equals("P.E.K.K.A");
    }

    private boolean isRangedTroop(Card c) {
        String n = c.getName();
        return n.equals("Musketeer") || n.equals("Archers") || n.equals("Spear Goblins") ||
                n.equals("Wizard") || n.equals("Witch");
    }

    private boolean isSwarm(Card c) {
        return c.getCount() >= 3;
    }

    private List<Troop> extractEnemyTroops(GameState gameState) {
        List<Troop> enemy = new ArrayList<>();
        for (Troop t : gameState.getActiveTroops()) {
            if (t != null && t.isAlive() && t.isPlayerSide()) {
                enemy.add(t);
            }
        }
        return enemy;
    }

    public ThreatReport analyzeThreats(List<Troop> enemyTroops) {
        if (enemyTroops == null || enemyTroops.isEmpty()) {
            return new ThreatReport(0, false, false, false, 0, null);
        }

        int threatElixir = 0;
        boolean hasAir = false;
        boolean hasSwarm = false;
        boolean hasBuildingOnly = false;
        Troop closest = null;

        for (Troop t : enemyTroops) {
            GridPosition p = t.getPosition();
            if (p == null) {
                continue;
            }

            if (p.getY() > com.kuroyale.util.config.GameConstants.RIVER_ROW_2 + 6) {
                continue;
            }

            Card base = t.getBaseCard();
            if (base != null) {
                threatElixir += Math.max(0, base.getCost());
                hasBuildingOnly |= (base.getTarget() == TargetType.BUILDINGS);
                hasSwarm |= (base.getCount() >= 3) || CardProfiles.hasRole(base, CardRole.SWARM);
            }
            hasAir |= t.isAirUnit();

            if (closest == null) {
                closest = t;
            } else {
                GridPosition cp = closest.getPosition();
                if (cp != null && p.getY() < cp.getY()) {
                    closest = t;
                }
            }
        }

        if (threatElixir <= 0) {
            return new ThreatReport(0, hasAir, hasSwarm, hasBuildingOnly, 0, null);
        }

        int lane = 0;
        Vector2 focus = null;
        if (closest != null && closest.getWorldPosition() != null) {
            focus = closest.getWorldPosition();
            lane = (focus.getX() < Arena.WIDTH / 2.0) ? 0 : 1;
        }

        return new ThreatReport(threatElixir, hasAir, hasSwarm, hasBuildingOnly, lane, focus);
    }

    private CandidateMove findBestDefenseMove(GameState gameState, ThreatReport threat) {
        List<Integer> playable = findAffordableHandIndices();
        if (playable.isEmpty()) {
            return null;
        }

        Card defensiveBuilding = null;
        int defensiveBuildingIdx = -1;
        for (int idx : playable) {
            Card c = hand.getCard(idx);
            if (c != null && c.getType() == CardType.BUILDING && CardProfiles.hasRole(c, CardRole.DEFENSIVE_BUILDING)) {
                defensiveBuilding = c;
                defensiveBuildingIdx = idx;
                break;
            }
        }

        if (threat.hasBuildingOnlyThreat && defensiveBuilding != null) {
            int x = Arena.WIDTH / 2;
            int y = com.kuroyale.util.config.GameConstants.RIVER_ROW_1 - 4;
            GridPosition p = findNearbyValidPlacement(gameState, defensiveBuilding, x, y);
            if (p != null) {
                return new CandidateMove(defensiveBuilding, defensiveBuildingIdx, p.getX(), p.getY(), 100.0);
            }
        }

        int centerX = Arena.WIDTH / 2;
        int pullX = centerX + (threat.lane == 0 ? -1 : 1);
        pullX = Math.max(0, Math.min(Arena.WIDTH - 1, pullX));

        int troopY = com.kuroyale.util.config.GameConstants.RIVER_ROW_1 - 2;
        int buildingY = com.kuroyale.util.config.GameConstants.RIVER_ROW_1 - 4;

        CandidateMove best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int idx : playable) {
            Card c = hand.getCard(idx);
            if (c == null) {
                continue;
            }
            if (c.getCost() > threat.threatElixir) {
                continue;
            }

            if (c.getType() == CardType.TROOP && c.getTarget() == TargetType.BUILDINGS) {
                continue;
            }

            if (threat.hasAirThreat) {
                if (c.getType() != CardType.SPELL && c.getTarget() != TargetType.BOTH
                        && c.getTarget() != TargetType.AIR) {
                    continue;
                }
                if (c.getType() == CardType.TROOP && !CardProfiles.hasRole(c, CardRole.RANGED_DPS)
                        && !CardProfiles.hasRole(c, CardRole.SPLASH_TROOP) && !c.isAreaEffect()) {
                    continue;
                }
            }

            double score = 10.0;
            if (threat.hasSwarmThreat) {
                if (CardProfiles.hasRole(c, CardRole.AOE_SPELL)) {
                    score += 30.0;
                }
                if (CardProfiles.hasRole(c, CardRole.SPLASH_TROOP) || c.isAreaEffect()) {
                    score += 20.0;
                }
            }

            if (threat.hasAirThreat && CardProfiles.hasRole(c, CardRole.RANGED_DPS)) {
                score += 20.0;
            }

            score += Math.min(10.0, (threat.threatElixir - c.getCost()));

            int desiredX = pullX;
            int desiredY = (c.getType() == CardType.BUILDING) ? buildingY : troopY;

            GridPosition p = findNearbyValidPlacement(gameState, c, desiredX, desiredY);
            if (p == null) {
                continue;
            }

            if (score > bestScore) {
                bestScore = score;
                best = new CandidateMove(c, idx, p.getX(), p.getY(), score);
            }
        }

        return best;
    }

    private CandidateMove findBestOffenseMove(GameState gameState) {
        if (elixirManager.getCurrentElixir() <= OFFENSE_ELIXIR_THRESHOLD) {
            return null;
        }

        List<Integer> playable = findAffordableHandIndices();
        if (playable.isEmpty()) {
            return null;
        }

        Troop crossingTank = findFriendlyTankCrossingBridge(gameState);
        if (crossingTank != null) {
            CandidateMove support = findSupportMoveBehindCrossingTank(gameState, playable, crossingTank);
            if (support != null) {
                return support;
            }
        }

        int giantIdx = -1;
        for (int idx : playable) {
            Card c = hand.getCard(idx);
            if (c != null && "Giant".equals(c.getName())) {
                giantIdx = idx;
                break;
            }
        }

        if (giantIdx >= 0) {
            Card giant = hand.getCard(giantIdx);
            GridPosition behindKing = getBehindKingPlacement(gameState);
            if (behindKing != null) {
                GridPosition p = findNearbyValidPlacement(gameState, giant, behindKing.getX(), behindKing.getY());
                if (p != null) {
                    return new CandidateMove(giant, giantIdx, p.getX(), p.getY(), 60.0);
                }
            }
        }

        return null;
    }

    private CandidateMove findSupportMoveBehindCrossingTank(GameState gameState, List<Integer> playable, Troop tank) {
        if (tank == null || tank.getWorldPosition() == null) {
            return null;
        }

        int desiredX = (int) Math.floor(tank.getWorldPosition().getX());
        desiredX = Math.max(0, Math.min(Arena.WIDTH - 1, desiredX));
        int desiredY = com.kuroyale.util.config.GameConstants.RIVER_ROW_1 - 1;

        Card best = null;
        int bestIdx = -1;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int idx : playable) {
            Card c = hand.getCard(idx);
            if (c == null) {
                continue;
            }
            if (c.getType() != CardType.TROOP) {
                continue;
            }
            if (CardProfiles.hasRole(c, CardRole.TANK) || c.getTarget() == TargetType.BUILDINGS) {
                continue;
            }
            double score = 10.0;
            if (CardProfiles.hasRole(c, CardRole.RANGED_DPS)) {
                score += 20.0;
            }
            if (CardProfiles.hasRole(c, CardRole.SPLASH_TROOP) || c.isAreaEffect()) {
                score += 10.0;
            }
            if (score > bestScore) {
                bestScore = score;
                best = c;
                bestIdx = idx;
            }
        }

        if (best == null) {
            return null;
        }

        GridPosition p = findNearbyValidPlacement(gameState, best, desiredX, desiredY);
        if (p == null) {
            return null;
        }
        return new CandidateMove(best, bestIdx, p.getX(), p.getY(), 40.0);
    }

    private Troop findFriendlyTankCrossingBridge(GameState gameState) {
        for (Troop t : gameState.getActiveTroops()) {
            if (t == null || !t.isAlive() || t.isPlayerSide()) {
                continue;
            }
            Card c = t.getBaseCard();
            if (c == null) {
                continue;
            }
            if (!"Giant".equals(c.getName())) {
                continue;
            }
            GridPosition p = t.getPosition();
            if (p == null) {
                continue;
            }
            if (p.getY() >= com.kuroyale.util.config.GameConstants.RIVER_ROW_2 + 1) {
                return t;
            }
        }
        return null;
    }

    private CandidateMove findBestSpellMove(GameState gameState, ThreatReport threat) {
        List<Integer> playable = findAffordableHandIndices();
        if (playable.isEmpty()) {
            return null;
        }

        List<Troop> enemies = extractEnemyTroops(gameState);
        if (enemies.isEmpty()) {
            return null;
        }

        CandidateMove best = null;
        double bestScore = Double.NEGATIVE_INFINITY;

        for (int idx : playable) {
            Card spell = hand.getCard(idx);
            if (spell == null || spell.getType() != CardType.SPELL) {
                continue;
            }

            SpellTarget bestTarget = findBestSpellTarget(gameState, spell, enemies);
            if (bestTarget == null) {
                continue;
            }

            double requiredValue = spell.getCost();
            if (bestTarget.value < requiredValue) {
                continue;
            }

            double score = (bestTarget.value - requiredValue) * 15.0;
            if (threat != null && threat.hasSwarmThreat && CardProfiles.hasRole(spell, CardRole.AOE_SPELL)) {
                score += 30.0;
            }
            if (score > bestScore) {
                bestScore = score;
                best = new CandidateMove(spell, idx, bestTarget.x, bestTarget.y, score);
            }
        }

        return best;
    }

    private SpellTarget findBestSpellTarget(GameState gameState, Card spell, List<Troop> enemies) {
        if (spell == null || enemies == null || enemies.isEmpty()) {
            return null;
        }
        double radius = Math.max(0.0, spell.getRange());
        if (radius <= 0.0) {
            return null;
        }

        boolean isProjectileSpell = "Fireball".equalsIgnoreCase(spell.getName())
                || "Rocket".equalsIgnoreCase(spell.getName());
        Vector2 start = new Vector2(Arena.WIDTH / 2.0, 0);
        Tower king = gameState.getArena().getKingTower(false);
        if (king != null && king.getCenterWorldPosition() != null) {
            start = king.getCenterWorldPosition();
        }

        SpellTarget best = null;
        for (Troop anchor : enemies) {
            if (anchor == null || !anchor.isAlive() || anchor.getWorldPosition() == null) {
                continue;
            }
            Vector2 center = anchor.getWorldPosition();

            double travelTime = 0.2;
            if (isProjectileSpell) {
                double dist = start.distanceTo(center);
                travelTime = dist / 15.0;
            }

            int value = 0;
            for (Troop t : enemies) {
                if (t == null || !t.isAlive() || t.getWorldPosition() == null) {
                    continue;
                }
                Vector2 predicted = predictTroopPosition(t, travelTime);
                if (predicted == null) {
                    continue;
                }
                if (center.distanceTo(predicted) <= radius) {
                    Card base = t.getBaseCard();
                    if (base != null) {
                        value += Math.max(0, base.getCost());
                    }
                }
            }

            GridPosition grid = center.toGridPosition();
            if (grid == null) {
                continue;
            }

            if (best == null || value > best.value) {
                best = new SpellTarget(grid.getX(), grid.getY(), value);
            }
        }
        return best;
    }

    private Vector2 predictTroopPosition(Troop troop, double timeSeconds) {
        if (troop == null || troop.getWorldPosition() == null) {
            return null;
        }
        if (timeSeconds <= 0) {
            return troop.getWorldPosition();
        }

        Vector2 pos = troop.getWorldPosition();
        Vector2 dir = null;

        if (troop.getPath() != null && !troop.getPath().isEmpty()) {
            Vector2 next = troop.getPath().peekFirst();
            if (next != null) {
                dir = next.subtract(pos).normalize();
            }
        }

        if (dir == null && troop.getTargetWorldPosition() != null) {
            dir = troop.getTargetWorldPosition().subtract(pos).normalize();
        }

        if (dir == null) {
            dir = troop.isPlayerSide() ? new Vector2(0, -1) : new Vector2(0, 1);
        }

        Vector2 predicted = pos.add(dir.multiply(troop.getMoveSpeed() * timeSeconds));
        double x = Math.max(0.0, Math.min(Arena.WIDTH - 1.0, predicted.getX()));
        double y = Math.max(0.0, Math.min(Arena.HEIGHT - 1.0, predicted.getY()));
        return new Vector2(x, y);
    }

    private CandidateMove findCycleMove(GameState gameState) {
        int bestIdx = -1;
        int bestCost = Integer.MAX_VALUE;
        for (int i = 0; i < Hand.HAND_SIZE; i++) {
            Card c = hand.getCard(i);
            if (c == null) {
                continue;
            }
            if (!elixirManager.canAfford(c.getCost())) {
                continue;
            }
            if (c.getType() == CardType.SPELL) {
                continue;
            }
            if (c.getType() == CardType.BUILDING && !"Elixir Collector".equals(c.getName())) {
                continue;
            }
            if (c.getCost() < bestCost) {
                bestCost = c.getCost();
                bestIdx = i;
            }
        }

        if (bestIdx < 0) {
            return null;
        }

        Card c = hand.getCard(bestIdx);

        GridPosition p;
        if (c != null && c.getType() == CardType.BUILDING && "Elixir Collector".equals(c.getName())) {
            p = getSafeCollectorPlacement(gameState);
        } else {
            p = getBehindKingPlacement(gameState);
        }
        if (p == null) {
            return null;
        }

        GridPosition valid = findNearbyValidPlacement(gameState, c, p.getX(), p.getY());
        if (valid == null) {
            return null;
        }
        return new CandidateMove(c, bestIdx, valid.getX(), valid.getY(), 1.0);
    }

    private List<Integer> findAffordableHandIndices() {
        List<Integer> affordable = new ArrayList<>();
        for (int i = 0; i < Hand.HAND_SIZE; i++) {
            Card card = hand.getCard(i);
            if (card != null && elixirManager.canAfford(card.getCost())) {
                affordable.add(i);
            }
        }
        return affordable;
    }

    private GridPosition getBehindKingPlacement(GameState gameState) {
        Tower king = gameState.getArena().getKingTower(false);
        if (king == null || king.getCenterPosition() == null) {
            return GridPosition.tryCreate(Arena.WIDTH / 2, 3);
        }
        GridPosition center = king.getCenterPosition();
        int x = center.getX();
        int y = Math.max(0, center.getY() - 3);
        return GridPosition.tryCreate(x, y);
    }

    private GridPosition getSafeCollectorPlacement(GameState gameState) {
        Tower king = gameState.getArena().getKingTower(false);
        if (king == null || king.getPosition() == null) {
            return GridPosition.tryCreate(Arena.WIDTH / 2, 3);
        }

        GridPosition topLeft = king.getPosition();
        int y = Math.max(0, topLeft.getY() - 2);
        int x = Math.max(2, Math.min(Arena.WIDTH - 3, topLeft.getX() + 2));
        return GridPosition.tryCreate(x, y);
    }

    private GridPosition getBotPrincessLaneAnchor(GameState gameState, int lane) {
        if (gameState == null || gameState.getArena() == null) {
            return null;
        }

        List<Tower> princess = gameState.getArena().getTowersByType(Tower.TowerType.PRINCESS, false);
        if (princess == null || princess.isEmpty()) {
            return null;
        }

        Tower left = null;
        Tower right = null;
        for (Tower t : princess) {
            if (t == null || t.getCenterPosition() == null) {
                continue;
            }
            if (left == null || t.getCenterPosition().getX() < left.getCenterPosition().getX()) {
                left = t;
            }
            if (right == null || t.getCenterPosition().getX() > right.getCenterPosition().getX()) {
                right = t;
            }
        }

        Tower chosen = (lane == 0) ? left : right;
        return chosen != null ? chosen.getCenterPosition() : null;
    }

    private GridPosition findNearbyValidPlacement(GameState gameState, Card card, int desiredX, int desiredY) {
        if (gameState == null || card == null) {
            return null;
        }

        int maxRadius = 4;
        GridPosition best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        int centerX = Arena.WIDTH / 2;
        boolean isCollector = card.getType() == CardType.BUILDING && "Elixir Collector".equals(card.getName());

        for (int r = 0; r <= maxRadius; r++) {
            for (int dx = -r; dx <= r; dx++) {
                for (int dy = -r; dy <= r; dy++) {
                    int x = desiredX + dx;
                    int y = desiredY + dy;
                    if (x < 0 || y < 0 || x >= Arena.WIDTH || y >= Arena.HEIGHT) {
                        continue;
                    }
                    if (!isValidPlacement(gameState, card, x, y)) {
                        continue;
                    }

                    int dist = Math.abs(x - desiredX) + Math.abs(y - desiredY);
                    int centerDist = Math.abs(x - centerX);

                    double score = 0.0;
                    score -= dist * 3.0;

                    if (card.getType() == CardType.BUILDING) {
                        if (isCollector) {
                            // Prefer safer (upper) placements for collector
                            score += (Arena.HEIGHT / 2.0 - y) * 2.0;
                            score -= centerDist * 0.5;
                        } else {
                            // Prefer centered + lower (toward river) placements for defensive buildings
                            score += y * 2.0;
                            score -= centerDist * 4.0;
                        }
                    } else {
                        // Troops: small bias to be closer to river but still near desired
                        score += y * 0.5;
                        score -= centerDist * 0.5;
                    }

                    if (score > bestScore) {
                        bestScore = score;
                        best = GridPosition.tryCreate(x, y);
                    }
                }
            }
        }

        return best;
    }

    private boolean isValidPlacement(GameState gameState, Card card, int x, int y) {
        if (gameState == null || card == null) {
            return false;
        }
        Arena arena = gameState.getArena();
        if (arena == null || !arena.isValidPosition(x, y)) {
            return false;
        }

        if (card.getType() == CardType.SPELL) {
            return true;
        }

        if (y >= Arena.HEIGHT / 2) {
            return false;
        }

        if (card.getType() == CardType.TROOP) {
            GridCell cell = arena.getCell(x, y);
            return cell != null && cell.canPlaceUnit();
        }

        if (card.getType() == CardType.BUILDING) {
            boolean isCollector = "Elixir Collector".equals(card.getName());
            if (!isCollector) {
                if (y < DEFENSIVE_BUILDING_MIN_Y || y > DEFENSIVE_BUILDING_MAX_Y) {
                    return false;
                }
            }

            int bw = Math.max(1, card.getFootprintWidthTiles());
            int bh = Math.max(1, card.getFootprintHeightTiles());
            int topLeftX = x - (bw / 2);
            int topLeftY = y - (bh / 2);

            if (topLeftX < 0 || topLeftY < 0 || (topLeftX + bw) > Arena.WIDTH || (topLeftY + bh) > Arena.HEIGHT) {
                return false;
            }

            if (!isCollector) {
                if (topLeftY < DEFENSIVE_BUILDING_MIN_Y || (topLeftY + bh - 1) > DEFENSIVE_BUILDING_MAX_Y) {
                    return false;
                }
            }

            for (int dx = 0; dx < bw; dx++) {
                for (int dy = 0; dy < bh; dy++) {
                    GridCell c = arena.getCell(topLeftX + dx, topLeftY + dy);
                    if (c == null || !c.canPlaceUnit()) {
                        return false;
                    }
                }
            }
            return true;
        }

        return false;
    }

    private int findCardIndexInHand(Card card) {
        if (card == null) {
            return -1;
        }
        for (int i = 0; i < Hand.HAND_SIZE; i++) {
            Card c = hand.getCard(i);
            if (card.equals(c)) {
                return i;
            }
        }
        return -1;
    }

    public ElixirManager getElixirManager() {
        return elixirManager;
    }

    public Hand getHand() {
        return hand;
    }

    public static class Move {
        public final Card card;
        public final int x;
        public final int y;

        public Move(Card card, int x, int y) {
            this.card = card;
            this.x = x;
            this.y = y;
        }
    }

    private static final class CandidateMove {
        private final Card card;
        private final int handIndex;
        private final int x;
        private final int y;
        private final double score;

        private CandidateMove(Card card, int handIndex, int x, int y, double score) {
            this.card = card;
            this.handIndex = handIndex;
            this.x = x;
            this.y = y;
            this.score = score;
        }
    }

    public static final class ThreatReport {
        public final int threatElixir;
        public final boolean hasAirThreat;
        public final boolean hasSwarmThreat;
        public final boolean hasBuildingOnlyThreat;
        public final int lane;
        public final Vector2 focusWorld;

        public ThreatReport(int threatElixir, boolean hasAirThreat, boolean hasSwarmThreat,
                boolean hasBuildingOnlyThreat,
                int lane, Vector2 focusWorld) {
            this.threatElixir = threatElixir;
            this.hasAirThreat = hasAirThreat;
            this.hasSwarmThreat = hasSwarmThreat;
            this.hasBuildingOnlyThreat = hasBuildingOnlyThreat;
            this.lane = lane;
            this.focusWorld = focusWorld;
        }
    }

    private static final class SpellTarget {
        private final int x;
        private final int y;
        private final int value;

        private SpellTarget(int x, int y, int value) {
            this.x = x;
            this.y = y;
            this.value = value;
        }
    }
}
