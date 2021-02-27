// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.logic.health;

import com.google.common.annotations.VisibleForTesting;
import org.joml.Vector3f;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.health.event.OnDamagedEvent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.logic.players.PlayerCharacterComponent;
import org.terasology.math.Direction;
import org.terasology.registry.In;
import org.terasology.rendering.nui.NUIManager;
import org.terasology.rendering.nui.layers.hud.DirectionalDamageOverlay;
import org.terasology.rendering.nui.layers.hud.MajorDamageOverlay;

@RegisterSystem(RegisterMode.CLIENT)
public class HealthClientSystem extends BaseComponentSystem {

    private static final float DAMAGE_OVERLAY_DELAY_SECONDS = 0.5f;
    private static final String DAMAGE_OVERLAY = "majorDamageOverlay";
    private static final float DAMAGE_OVERLAY_REQUIRED_PERCENT = 0.25f;

    @In
    private NUIManager nuiManager;

    private boolean overlayDisplaying;
    private DirectionalDamageOverlay directionalDamageOverlay;

    @Override
    public void initialise() {
        nuiManager.getHUD().addHUDElement("healthHud");
        directionalDamageOverlay = (DirectionalDamageOverlay) nuiManager.getHUD().addHUDElement("directionalDamageOverlay");
    }

    @ReceiveEvent(components = PlayerCharacterComponent.class)
    public void onDamaged(OnDamagedEvent event, EntityRef entity,
                          LocationComponent locationComponent,
                          HealthComponent healthComponent) {
        // Show the relevant direction element
        EntityRef instigator = event.getInstigator();
        if (instigator != null && instigator.hasComponent(LocationComponent.class)) {
            Direction direction = determineDamageDirection(instigator, locationComponent);
            directionalDamageOverlay.show(direction, DAMAGE_OVERLAY_DELAY_SECONDS);
        }

        // Is it major damage?
        int amount = event.getDamageAmount();
        int current = healthComponent.currentHealth;
        if (current > 0 && amount > 0) {
            float percent = (float) amount / current;
            if (percent >= DAMAGE_OVERLAY_REQUIRED_PERCENT && !overlayDisplaying) {
                overlayDisplaying = true;
                nuiManager.addOverlay(DAMAGE_OVERLAY, MajorDamageOverlay.class);
            }
        }
    }

    @VisibleForTesting
    Direction determineDamageDirection(EntityRef instigator, LocationComponent locationComponent) {
        LocationComponent instigatorLocation = instigator.getComponent(LocationComponent.class);
        Vector3f loc = locationComponent.getWorldPosition(new Vector3f());
        Vector3f locDiff = instigatorLocation.getWorldPosition(new Vector3f()).sub(loc).normalize();

        // facing x and z are "how much" of that direction we are facing
        // e.g. (0.0, 1.0) means that going forward increases world z position without increasing x position
        Vector3f worldFacing = locationComponent.getWorldDirection(new Vector3f()).normalize();

        double direction = Math.toDegrees(worldFacing.angleSigned(locDiff, Direction.UP.asVector3f()));

        if (direction <= 45.0 && direction > -45.0) {
            return Direction.FORWARD;
        } else if (direction <= -45.0 && direction > -135.0) {
            return Direction.RIGHT;
        } else if (direction <= 135.0 && direction > 45.0) {
            return Direction.LEFT;
        } else {
            return Direction.BACKWARD;
        }
    }
}
