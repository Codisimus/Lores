package com.codisimus.plugins.lores;

import java.util.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.plugin.java.JavaPlugin;

public class Lores extends JavaPlugin implements CommandExecutor {
    private static enum Action { NAME, OWNER, ADD, DELETE, SET, INSERT, CLEAR, UNDO }
    private static HashMap<String, LinkedList<ItemStack>> undo = new HashMap<String, LinkedList<ItemStack>>();

    @Override
    public void onEnable () {
        //Register the lore command
        getCommand("lore").setExecutor(this);

        //Retrieve the version file
        Properties version = new Properties();
        try {
            version.load(this.getResource("version.properties"));
        } catch (Exception ex) {
        }

        //Log the version and build numbers
        getLogger().info("Shortcuts " + this.getDescription().getVersion()
                + " (Build " + version.getProperty("Build") + ") is enabled!");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        //This Command is only useful to Players
        if (!(sender instanceof Player)) {
            return false;
        }
        Player player = (Player) sender;

        ItemStack item = player.getItemInHand();
        ItemMeta meta = item.getItemMeta();

        //There must be at least one argument
        if (args.length < 1) {
            return false;
        }

        //Retrieve the lore and make one if it doesn't exist
        List<String> lore = meta.getLore();
        if (lore == null) {
            lore = new LinkedList<String>();
        }

        //Discover the action to be executed
        Action action;
        try {
            action = Action.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException notEnum) {
            return false;
        }

        //Generate the String id for the Player/Item
        String id = player.getName() + "'" + item.getTypeId();

        if (action != Action.UNDO) {
            //Create an undo List for the Player if there isn't one
            if (!undo.containsKey(id)) {
                undo.put(id, new LinkedList<ItemStack>());
            }

            //Add the meta state to the undo List
            LinkedList<ItemStack> list = undo.get(id);
            list.addFirst(item.clone());

            //Don't allow the undo List to grow too large
            while (list.size() > 5) {
                list.removeLast();
            }
        }

        switch (action) {
        case NAME: //Change the Name of the Item
            //Ensure that we are given the correct number of arguments
            if (args.length < 2) {
                return false;
            }

            String name = concatArgs(args, 1);
            if (name.contains("|")) {
                int max = name.replaceAll("§[0-9a-klmnor]", "").length();
                Iterator<String> itr = lore.iterator();
                while (itr.hasNext()) {
                    max = Math.max(max, itr.next().replaceAll("§[0-9a-klmnor]", "").length());
                }
                int spaces = max - name.replaceAll("§[0-9a-klmnor]", "").length() - 1;
                String space = " ";
                for (int i = 1; i < spaces * 1.5; i++) {
                    space += ' ';
                }
                name = name.replace("|", space);
            }

            meta.setDisplayName(name);
            break;

        case OWNER: //Change the Owner of the Skull
            //Ensure that we are given the correct number of arguments
            if (args.length != 2) {
                return false;
            }

            if (!(meta instanceof SkullMeta)) {
                player.sendMessage("§4You may only set the Owner of a §6Skull");
                return true;
            }

            ((SkullMeta) meta).setOwner(args[1]);
            break;

        case ADD: //Add a line to the end of the lore
            //Ensure that we are given the correct number of arguments
            if (args.length < 2) {
                return false;
            }

            lore.add(concatArgs(args, 1));
            break;

        case DELETE: //Delete a line of the lore
            switch (args.length) {
            case 1: //Delete the last line
                if (lore.size() < 1) {
                    player.sendMessage("§4There is nothing to delete!");
                    return true;
                }

                lore.remove(lore.size() - 1);
                break;

            case 2: //Delete specified line
                //Ensure that the argument is an Integer
                int index;
                try {
                    index = Integer.parseInt(args[1]) - 1;
                } catch (Exception e) {
                    return false;
                }

                //Ensure that the lore is large enough
                if (lore.size() <= index || index < 0) {
                    player.sendMessage("§4Invalid line number!");
                    return true;
                }

                lore.remove(index);
                break;

            default: return false;
            }
            break;

        case SET: //Change a line of the lore
            //Ensure that we are given the correct number of arguments
            if (args.length < 3) {
                return false;
            }

            //Ensure that the argument is an Integer
            int index;
            try {
                index = Integer.parseInt(args[1]) - 1;
            } catch (Exception e) {
                return false;
            }

            //Ensure that the lore is large enough
            if (lore.size() <= index || index < 0) {
                player.sendMessage("§4Invalid line number!");
                return true;
            }

            lore.set(index, concatArgs(args, 2));
            break;

        case INSERT: //Insert a line into the lore
            //Ensure that we are given the correct number of arguments
            if (args.length < 3) {
                return false;
            }

            //Ensure that the argument is an Integer
            int i;
            try {
                i = Integer.parseInt(args[1]) - 1;
            } catch (Exception e) {
                return false;
            }

            //Ensure that the lore is large enough
            if (lore.size() <= i || i < 0) {
                player.sendMessage("§4Invalid line number!");
                return true;
            }

            lore.add(i, concatArgs(args, 2));
            break;

        case CLEAR:
            //Ensure that we are given the correct number of arguments
            if (args.length != 1) {
                return false;
            }

            lore.clear();
            break;

        case UNDO:
            //Ensure that we are given the correct number of arguments
            if (args.length != 1) {
                return false;
            }

            //Retrieve the old ItemStack from the undo List
            LinkedList<ItemStack> list = undo.get(id);
            if (list == null) {
                player.sendMessage("§4You have not yet modified this Item!");
                return true;
            }
            if (list.size() < 1) {
                player.sendMessage("§4You cannot continue to undo for this Item!");
                return true;
            }

            //Place the old Item in the Player's hand
            player.setItemInHand(list.removeFirst());
            player.sendMessage("§5The last modification you made on this item has been undone!");
            return true;
        }

        //Set the new lore/meta
        meta.setLore(lore);
        item.setItemMeta(meta);
        player.sendMessage("§5Lore successfully modified!");
        return true;
    }

    /**
     * Concats arguments together to create a sentence from words
     * This also replaces & with § to add color codes to the String
     *
     * @param args the arguments to concat
     * @param first Which argument should the sentence start with
     * @return The new String that was created
     */
    private static String concatArgs(String[] args, int first) {
        String string = "";
        for (int i = first; i <= args.length - 1; i++) {
            string += " " + args[i].replace('&', '§');
        }
        string = string.replaceAll("§[lnokm]", "");
        return string.isEmpty() ? string : string.substring(1);
    }
}
