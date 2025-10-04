package guiclicker.event;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C10PacketCreativeInventoryAction;
import net.minecraft.util.ChatComponentText;
import net.minecraftforge.client.event.GuiScreenEvent.KeyboardInputEvent;
import net.minecraftforge.client.event.GuiScreenEvent.MouseInputEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.SlotItemHandler;
import guiclicker.config.Configs;

@SideOnly(Side.CLIENT)
public class InputEventHandler
{
    private int lastPosX;
    private int lastPosY;
    private int slotNumberLast;
    private ItemStack[] originalStacks = new ItemStack[0];
    private final Set<Integer> draggedSlots = new HashSet<Integer>();

    @SubscribeEvent
    public void onMouseInputEvent(MouseInputEvent.Pre event)
    {
        if (event.gui instanceof GuiContainer)
        {
            boolean cancel = false;

            if (Configs.enableDragMovingShiftLeft || Configs.enableDragMovingShiftRight || Configs.enableDragMovingControlLeft)
            {
                cancel = this.dragMoveItems((GuiContainer)event.gui);
            }

            if (cancel && event.isCancelable())
            {
                event.setCanceled(true);
            }
        }
        else
        {
            // Clear state when not in a container GUI to prevent stuck states
            this.draggedSlots.clear();
            this.slotNumberLast = -1;
        }
    }

    @SubscribeEvent
    public void onKeyboardInputEvent(KeyboardInputEvent.Pre event)
    {
        if (event.gui instanceof GuiContainer)
        {
            boolean cancel = false;
            
            // Check for Q key press with modifiers
            if (Keyboard.getEventKeyState() && Keyboard.getEventKey() == Keyboard.KEY_Q)
            {
                cancel = this.handleDropKeybind((GuiContainer)event.gui);
            }

            if (cancel && event.isCancelable())
            {
                event.setCanceled(true);
            }
        }
    }

    private boolean isValidSlot(Slot slot, GuiContainer gui, boolean requireItems)
    {
        if (gui.inventorySlots == null || gui.inventorySlots.inventorySlots == null)
        {
            return false;
        }

        return slot != null && gui.inventorySlots.inventorySlots.contains(slot) == true && (requireItems == false || slot.getHasStack() == true);
    }

    private boolean dragMoveItems(GuiContainer gui)
    {
        boolean isShiftDown = GuiContainer.isShiftKeyDown();
        boolean isControlDown = GuiContainer.isCtrlKeyDown();
        
        // Early exit if none of the drag features are enabled
        if (!Configs.enableDragMovingShiftLeft && !Configs.enableDragMovingShiftRight && !Configs.enableDragMovingControlLeft)
        {
            return false;
        }
        
        // Early exit if no modifier keys are pressed
        if (!isShiftDown && !isControlDown)
        {
            return false;
        }
        
        boolean leftButtonDown = Mouse.isButtonDown(0);
        boolean rightButtonDown = Mouse.isButtonDown(1);
        boolean eitherMouseButtonDown = leftButtonDown || rightButtonDown;
        boolean eventKeyIsLeftButton = (Mouse.getEventButton() - 100) == gui.mc.gameSettings.keyBindAttack.getKeyCode();
        boolean eventKeyIsRightButton = (Mouse.getEventButton() - 100) == gui.mc.gameSettings.keyBindUseItem.getKeyCode();
        boolean eventButtonState = Mouse.getEventButtonState();

        boolean leaveOneItem = leftButtonDown == false;
        boolean moveOnlyOne = isShiftDown == false;
        int mouseX = (Mouse.getEventX() * gui.width / gui.mc.displayWidth) - gui.guiLeft;
        int mouseY = (gui.height - Mouse.getEventY() * gui.height / gui.mc.displayHeight - 1) - gui.guiTop;
        boolean cancel = false;
        Slot slot = this.getSlotAtPosition(gui, mouseX, mouseY);

        if (eventButtonState == true)
        {
            if (((eventKeyIsLeftButton || eventKeyIsRightButton) && isControlDown && Configs.enableDragMovingControlLeft) ||
                (eventKeyIsRightButton && isShiftDown && Configs.enableDragMovingShiftRight) ||
                (eventKeyIsLeftButton && isShiftDown && Configs.enableDragMovingShiftLeft))
            {
                // Reset this or the method call won't do anything...
                this.slotNumberLast = -1;
                this.dragMoveFromSlotAtPosition(gui, mouseX, mouseY, leaveOneItem, moveOnlyOne);
                cancel = true;
            }
        }

        // Check that either mouse button is down
        if (cancel == false && (isShiftDown == true || isControlDown == true) && eitherMouseButtonDown == true)
        {
            int distX = mouseX - this.lastPosX;
            int distY = mouseY - this.lastPosY;
            int absX = Math.abs(distX);
            int absY = Math.abs(distY);

            if (absX > absY)
            {
                int inc = distX > 0 ? 1 : -1;

                for (int x = this.lastPosX; ; x += inc)
                {
                    int y = absX != 0 ? this.lastPosY + ((x - this.lastPosX) * distY / absX) : mouseY;
                    this.dragMoveFromSlotAtPosition(gui, x, y, leaveOneItem, moveOnlyOne);

                    if (x == mouseX)
                    {
                        break;
                    }
                }
            }
            else
            {
                int inc = distY > 0 ? 1 : -1;

                for (int y = this.lastPosY; ; y += inc)
                {
                    int x = absY != 0 ? this.lastPosX + ((y - this.lastPosY) * distX / absY) : mouseX;
                    this.dragMoveFromSlotAtPosition(gui, x, y, leaveOneItem, moveOnlyOne);

                    if (y == mouseY)
                    {
                        break;
                    }
                }
            }
        }

        this.lastPosX = mouseX;
        this.lastPosY = mouseY;

        // Always update the slot under the mouse.
        // This should prevent a "double click/move" when shift + left clicking on slots that have more
        // than one stack of items. (the regular slotClick() + a "drag move" from the slot that is under the mouse
        // when the left mouse button is pressed down and this code runs).
        this.slotNumberLast = slot != null ? slot.slotNumber : -1;

        // Clear dragged slots when no mouse buttons are pressed to prevent stuck states
        if (eitherMouseButtonDown == false)
        {
            this.draggedSlots.clear();
        }

        return cancel;
    }

    private void dragMoveFromSlotAtPosition(GuiContainer gui, int x, int y, boolean leaveOneItem, boolean moveOnlyOne)
    {
        Slot slot = this.getSlotAtPosition(gui, x, y);

        if (slot != null && slot.slotNumber != this.slotNumberLast)
        {
            if (this.isValidSlot(slot, gui, true) == true)
            {
                if (moveOnlyOne == true)
                {
                    if (this.draggedSlots.contains(slot.slotNumber) == false)
                    {
                        this.tryMoveSingleItemToOtherInventory(slot, gui);
                        this.draggedSlots.add(slot.slotNumber);
                    }
                }
                else
                {
                    if (this.draggedSlots.contains(slot.slotNumber) == false)
                    {
                        if (leaveOneItem == true)
                        {
                            this.tryMoveAllButOneItemToOtherInventory(slot, gui);
                        }
                        else
                        {
                            this.shiftClickSlot(gui.inventorySlots, gui.mc, slot.slotNumber);
                        }

                        this.draggedSlots.add(slot.slotNumber);
                    }
                }
            }
        }
    }

    private boolean isMouseOverSlot(Slot slot, int mouseX, int mouseY)
    {
        return this.isPointInRegion(slot.xDisplayPosition, slot.yDisplayPosition, 16, 16, mouseX, mouseY);
    }

    private boolean isPointInRegion(int left, int top, int width, int height, int pointX, int pointY)
    {
        return pointX >= left - 1 && pointX < left + width + 1 && pointY >= top - 1 && pointY < top + height + 1;
    }

    private Slot getSlotAtPosition(GuiContainer gui, int x, int y)
    {
        for (int i = 0; i < gui.inventorySlots.inventorySlots.size(); i++)
        {
            Slot slot = gui.inventorySlots.inventorySlots.get(i);

            if (this.isMouseOverSlot(slot, x, y) == true)
            {
                return slot;
            }
        }

        return null;
    }

    private void tryMoveSingleItemToOtherInventory(Slot slot, GuiContainer gui)
    {
        ItemStack stackOrig = slot.getStack();

        if (stackOrig == null || slot.canTakeStack(gui.mc.thePlayer) == false || (stackOrig.stackSize > 1 && slot.isItemValid(stackOrig) == false))
        {
            return;
        }

        Container container = gui.inventorySlots;
        ItemStack stack = stackOrig.copy();
        stack.stackSize = 1;

        this.originalStacks = this.getOriginalStacks(container);

        // Try to move the temporary single-item stack via the shift-click handler method
        slot.putStack(stack);
        container.transferStackInSlot(gui.mc.thePlayer, slot.slotNumber);

        // Successfully moved the item somewhere, now we want to check where it went
        if (slot.getHasStack() == false)
        {
            int targetSlot = this.getTargetSlot(container);

            // Found where the item went
            if (targetSlot >= 0)
            {
                // Remove the dummy item from the target slot (on the client side)
                container.inventorySlots.get(targetSlot).decrStackSize(1);

                // Restore the original stack to the slot under the cursor (on the client side)
                slot.putStack(stackOrig);

                // Do the slot clicks to actually move the items (on the server side)
                this.clickSlotsToMoveSingleItem(container, gui.mc, slot.slotNumber, targetSlot);
                return;
            }
        }

        // Restore the original stack to the slot under the cursor (on the client side)
        slot.putStack(stackOrig);
    }

    private void tryMoveAllButOneItemToOtherInventory(Slot slot, GuiContainer gui)
    {
        ItemStack stackOrig = slot.getStack();

        if (stackOrig == null || stackOrig.stackSize == 1 || slot.canTakeStack(gui.mc.thePlayer) == false || slot.isItemValid(stackOrig) == false)
        {
            return;
        }

        Container container = gui.inventorySlots;
        Slot emptySlot = findEmptySlotInSameInventory(slot, container, stackOrig);

        if (emptySlot != null)
        {
            // Take the stack by left clicking
            gui.mc.playerController.windowClick(container.windowId, slot.slotNumber, 0, 0, gui.mc.thePlayer);

            // Return one item by right clicking
            gui.mc.playerController.windowClick(container.windowId, slot.slotNumber, 1, 0, gui.mc.thePlayer);

            // Put the rest of the items into the empty slot
            gui.mc.playerController.windowClick(container.windowId, emptySlot.slotNumber, 0, 0, gui.mc.thePlayer);

            // Shift click the stack
            this.shiftClickSlot(container, gui.mc, emptySlot.slotNumber);

            if (emptySlot.getHasStack() == true)
            {
                // If items remain after the shift click, pick them up and return them to the original slot
                gui.mc.playerController.windowClick(container.windowId, emptySlot.slotNumber, 0, 0, gui.mc.thePlayer);
                gui.mc.playerController.windowClick(container.windowId, slot.slotNumber, 0, 0, gui.mc.thePlayer);
            }
        }
    }

    private void moveItemsFromInventory(GuiContainer gui, int slotTo, IInventory invSrc, ItemStack stackTemplate, boolean fillStacks)
    {
        Container container = gui.inventorySlots;

        for (Slot slot : container.inventorySlots)
        {
            if (slot == null)
            {
                continue;
            }

            if (slot.inventory == invSrc && areStacksEqual(stackTemplate, slot.getStack()) == true)
            {
                if (fillStacks == true)
                {
                    if (this.clickSlotsToMoveItems(container, gui.mc, slot.slotNumber, slotTo) == false)
                    {
                        break;
                    }
                }
                else
                {
                    this.clickSlotsToMoveSingleItem(container, gui.mc, slot.slotNumber, slotTo);
                    break;
                }
            }
        }
    }

    private static boolean areStacksEqual(ItemStack stack1, ItemStack stack2)
    {
        return ItemStack.areItemsEqual(stack1, stack2) && ItemStack.areItemStackTagsEqual(stack1, stack2);
    }

    private static boolean areSlotsInSameInventory(Slot slot1, Slot slot2)
    {
        if ((slot1 instanceof SlotItemHandler) && (slot2 instanceof SlotItemHandler))
        {
            return ((SlotItemHandler)slot1).itemHandler == ((SlotItemHandler)slot2).itemHandler;
        }

        return slot1.inventory == slot2.inventory;
    }

    private Slot findEmptySlotInSameInventory(Slot slot, Container container, ItemStack stack)
    {
        for (Slot slotTmp : container.inventorySlots)
        {
            if (areSlotsInSameInventory(slotTmp, slot) == true &&
                slotTmp.getHasStack() == false && slotTmp.isItemValid(stack) == true)
            {
                return slotTmp;
            }
        }

        return null;
    }

    private ItemStack[] getOriginalStacks(Container container)
    {
        ItemStack[] originalStacks = new ItemStack[container.inventorySlots.size()];

        for (int i = 0; i < originalStacks.length; i++)
        {
            originalStacks[i] = ItemStack.copyItemStack(container.inventorySlots.get(i).getStack());
        }

        return originalStacks;
    }

    private int getTargetSlot(Container container)
    {
        List<Slot> slots = container.inventorySlots;

        for (int i = 0; i < this.originalStacks.length; i++)
        {
            ItemStack stackOrig = this.originalStacks[i];
            ItemStack stackNew = slots.get(i).getStack();

            if ((stackOrig == null && stackNew != null) ||
               (stackOrig != null && stackNew != null && stackNew.stackSize == (stackOrig.stackSize + 1)))
            {
                return i;
            }
        }

        return -1;
    }

    private void shiftClickSlot(Container container, Minecraft mc, int slot)
    {
        mc.playerController.windowClick(container.windowId, slot, 0, 1, mc.thePlayer);
    }

    private void clickSlotsToMoveSingleItem(Container container, Minecraft mc, int slotFrom, int slotTo)
    {
        EntityPlayer player = mc.thePlayer;
        //System.out.println("clickSlotsToMoveSingleItem(from: " + slotFrom + ", to: " + slotTo + ")");

        ItemStack stack = container.inventorySlots.get(slotFrom).getStack();
        boolean moreThanOne = stack != null && stack.stackSize > 1;

        // Right click on the from-slot to take items to the cursor. If it's the last item, then left click instead.
        mc.playerController.windowClick(container.windowId, slotFrom, moreThanOne == true ? 1 : 0, 0, player);

        // Right click on the target slot to put one item to it
        mc.playerController.windowClick(container.windowId, slotTo, 1, 0, player);

        // If there are items left in the cursor, then return them back to the original slot
        if (player.inventory.getItemStack() != null)
        {
            // Left click again on the from-slot to return the rest of the items to it
            mc.playerController.windowClick(container.windowId, slotFrom, 0, 0, player);
        }
    }

    /**
     * Try move items from slotFrom to slotTo
     * @return true if at least some items were moved
     */
    private boolean clickSlotsToMoveItems(Container container, Minecraft mc, int slotFrom, int slotTo)
    {
        EntityPlayer player = mc.thePlayer;
        //System.out.println("clickSlotsToMoveItems(from: " + slotFrom + ", to: " + slotTo + ")");

        // Left click to take items
        mc.playerController.windowClick(container.windowId, slotFrom, 0, 0, player);

        // Couldn't take the items, bail out now
        if (player.inventory.getItemStack() == null)
        {
            return false;
        }

        boolean ret = true;
        int size = player.inventory.getItemStack().stackSize;

        // Left click on the target slot to put the items to it
        mc.playerController.windowClick(container.windowId, slotTo, 0, 0, player);

        // If there are items left in the cursor, then return them back to the original slot
        if (player.inventory.getItemStack() != null)
        {
            ret = player.inventory.getItemStack().stackSize != size;

            // Left click again on the from-slot to return the rest of the items to it
            mc.playerController.windowClick(container.windowId, slotFrom, 0, 0, player);
        }

        return ret;
    }

    private boolean handleDropKeybind(GuiContainer gui)
    {
        // Use direct keyboard state checking for better Mac compatibility
        boolean isShiftDown = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        boolean isControlDown = Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL) || 
                               Keyboard.isKeyDown(Keyboard.KEY_LMETA) || Keyboard.isKeyDown(Keyboard.KEY_RMETA); // Mac Cmd keys
        
        // Get mouse position to determine hovered slot
        int mouseX = (Mouse.getX() * gui.width / gui.mc.displayWidth) - gui.guiLeft;
        int mouseY = (gui.height - Mouse.getY() * gui.height / gui.mc.displayHeight - 1) - gui.guiTop;
        Slot hoveredSlot = this.getSlotAtPosition(gui, mouseX, mouseY);
        
        if (hoveredSlot == null || !hoveredSlot.getHasStack())
        {
            return false;
        }
        
        // CTRL + SHIFT + Q = drop all items of same type
        if (isControlDown && isShiftDown && Configs.enableDropAllOfType)
        {
            return this.dropAllItemsOfType(gui, hoveredSlot);
        }
        // CTRL + Q = drop entire stack
        else if (isControlDown && !isShiftDown && Configs.enableDropStack)
        {
            return this.dropStack(gui, hoveredSlot);
        }
        
        return false;
    }

    private boolean dropStack(GuiContainer gui, Slot slot)
    {
        if (slot == null || !slot.getHasStack())
        {
            return false;
        }
        
        EntityPlayer player = gui.mc.thePlayer;
        Container container = gui.inventorySlots;
        
        // Left click to pick up the stack
        gui.mc.playerController.windowClick(container.windowId, slot.slotNumber, 0, 0, player);
        
        // Drop the stack (Q key simulation)
        if (player.inventory.getItemStack() != null)
        {
            gui.mc.playerController.windowClick(container.windowId, -999, 0, 0, player);
            return true;
        }
        
        return false;
    }

    private boolean dropAllItemsOfType(GuiContainer gui, Slot referenceSlot)
    {
        if (referenceSlot == null || !referenceSlot.getHasStack())
        {
            return false;
        }
        
        ItemStack referenceStack = referenceSlot.getStack();
        EntityPlayer player = gui.mc.thePlayer;
        Container container = gui.inventorySlots;
        boolean droppedAny = false;
        
        // Go through all slots and drop matching items
        for (Slot slot : (List<Slot>) container.inventorySlots)
        {
            if (slot.getHasStack() && this.areItemStacksEqual(slot.getStack(), referenceStack))
            {
                // Left click to pick up the stack
                gui.mc.playerController.windowClick(container.windowId, slot.slotNumber, 0, 0, player);
                
                // Drop the stack
                if (player.inventory.getItemStack() != null)
                {
                    gui.mc.playerController.windowClick(container.windowId, -999, 0, 0, player);
                    droppedAny = true;
                }
            }
        }
        
        return droppedAny;
    }

    private boolean areItemStacksEqual(ItemStack stack1, ItemStack stack2)
    {
        if (stack1 == null || stack2 == null)
        {
            return false;
        }
        
        return stack1.getItem() == stack2.getItem() && 
               stack1.getItemDamage() == stack2.getItemDamage() &&
               ItemStack.areItemStackTagsEqual(stack1, stack2);
    }
}
