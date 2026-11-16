package dev.strangevisuals.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.strangevisuals.hands.HandEditor;
import dev.strangevisuals.hands.HandPose;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Unique
    private static final float PIVOT_X = HandEditor.PIVOT_X;
    @Unique
    private static final float PIVOT_Y = HandEditor.PIVOT_Y;
    @Unique
    private static final float PIVOT_Z = HandEditor.PIVOT_Z;

    @Unique
    private static final float BOX_X = 0.26f;
    @Unique
    private static final float BOX_UP = 0.3f;
    @Unique
    private static final float BOX_DOWN = 0.42f;
    @Unique
    private static final float BOX_Z = 0.2f;

    @ModifyExpressionValue(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;"
        + "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;"
        + "Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
        at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer$HandRenderType;"
                + "renderMainHand:Z"))
    private boolean strangevisuals$alwaysMainHand(boolean original) {
        return original || HandEditor.active();
    }

    @ModifyExpressionValue(method = "renderItem(FLnet/minecraft/client/util/math/MatrixStack;"
        + "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;"
        + "Lnet/minecraft/client/network/ClientPlayerEntity;I)V",
        at = @At(value = "FIELD",
            target = "Lnet/minecraft/client/render/item/HeldItemRenderer$HandRenderType;"
                + "renderOffHand:Z"))
    private boolean strangevisuals$alwaysOffHand(boolean original) {
        return original || HandEditor.active();
    }

    @Shadow
    protected abstract void renderArmHoldingItem(MatrixStack matrices,
                                                 OrderedRenderCommandQueue queue, int light,
                                                 float equipProgress, float swingProgress, Arm arm);

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true)
    private void strangevisuals$placeHand(AbstractClientPlayerEntity player, float tickProgress,
                                          float pitch, Hand hand, float swingProgress,
                                          ItemStack stack, float equipProgress, MatrixStack matrices,
                                          OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        Arm arm = hand == Hand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
        float side = arm == Arm.RIGHT ? 1.0f : -1.0f;

        float pivotX = side * PIVOT_X;
        matrices.push();

        float scale = HandPose.scale(arm);

        matrices.translate(pivotX + HandPose.x(arm), PIVOT_Y + HandPose.y(arm),
            PIVOT_Z + HandPose.z(arm));
        if (Math.abs(scale - 1.0f) > 0.0001f) {
            matrices.scale(scale, scale, scale);
        }
        matrices.translate(-pivotX, -PIVOT_Y, -PIVOT_Z);

        if (HandEditor.active()) {
            if (stack.isEmpty() && hand == Hand.OFF_HAND && !player.isInvisible()) {
                renderArmHoldingItem(matrices, queue, light, equipProgress, swingProgress, arm);
                matrices.pop();
                ci.cancel();
            }
        }
    }

        @Inject(method = "renderFirstPersonItem", at = @At("RETURN"))
    private void strangevisuals$releaseHand(AbstractClientPlayerEntity player, float tickProgress,
                                            float pitch, Hand hand, float swingProgress,
                                            ItemStack stack, float equipProgress, MatrixStack matrices,
                                            OrderedRenderCommandQueue queue, int light, CallbackInfo ci) {
        matrices.pop();
    }
}
