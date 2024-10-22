package dev.gigaherz.enderrift.client;

import dev.gigaherz.enderrift.EnderRiftMod;
import dev.gigaherz.enderrift.rift.RiftRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = EnderRiftMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientHelper
{
    @SubscribeEvent
    public static void registerModels(ModelEvent.RegisterAdditional event)
    {
        event.register(EnderRiftMod.location("block/sphere"));
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event)
    {
        BlockEntityRenderers.register(EnderRiftMod.RIFT_BLOCK_ENTITY.get(), RiftRenderer::new);
    }

}