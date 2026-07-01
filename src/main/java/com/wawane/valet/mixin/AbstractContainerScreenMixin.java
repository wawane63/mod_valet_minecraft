package com.wawane.valet.mixin;

import com.wawane.valet.network.packets.SortContainerPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin<T extends AbstractContainerMenu> extends Screen {
    @Shadow
    @Final
    protected T menu;

    @Shadow
    @Final
    protected int imageWidth;

    @Shadow
    protected int leftPos;

    @Shadow
    protected int topPos;

    protected AbstractContainerScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void valet$addSortButton(CallbackInfo ci) {
        if (!((Object) this instanceof ContainerScreen) || !(menu instanceof ChestMenu)) {
            return;
        }

        Component label = Component.translatable("screen.valet.sort_short");
        addRenderableWidget(Button.builder(label, button -> ClientPlayNetworking.send(SortContainerPayload.INSTANCE))
                .bounds(leftPos + imageWidth - 46, topPos + 4, 38, 18)
                .tooltip(Tooltip.create(Component.translatable("screen.valet.sort_container")))
                .build());
    }
}
