package gigaherz.enderRift.integration;

import gigaherz.enderRift.gui.ContainerCraftingBrowser;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.recipe.VanillaRecipeCategoryUid;

import javax.annotation.Nonnull;

@mezz.jei.api.JEIPlugin
public class JEIPlugin implements IModPlugin
{
    @Override
    public void register(@Nonnull IModRegistry registry)
    {
        registry.getRecipeTransferRegistry()
                .addRecipeTransferHandler(ContainerCraftingBrowser.class, VanillaRecipeCategoryUid.CRAFTING,
                        ContainerCraftingBrowser.CraftingSlotStart, 9,
                        ContainerCraftingBrowser.InventorySlotStart, 36);
    }

    @Override
    public void onRuntimeAvailable(@Nonnull IJeiRuntime jeiRuntime)
    {
    }
}
