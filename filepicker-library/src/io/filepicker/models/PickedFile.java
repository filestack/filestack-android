package io.filepicker.models;

import java.util.ArrayList;

/**
 * Created by maciejwitowski on 10/30/14.
 */
public final class PickedFile {

    public final Node node;

    public PickedFile(Node node) {
        this.node = node;
    }

    public static ArrayList<Node> getNodes(ArrayList<PickedFile> pickedFiles) {
        ArrayList<Node> nodes = new ArrayList<>();
        for (PickedFile file : pickedFiles) {
            nodes.add(file.node);
        }
        return nodes;
    }

    public static void removeNode(ArrayList<PickedFile> pickedFiles, Node node) {
        for (PickedFile file : pickedFiles) {
            if (file.node == node) {
                pickedFiles.remove(file);
                break;
            }
        }
    }

    public static boolean containsNode(ArrayList<PickedFile> pickedFiles, Node node) {
        for (PickedFile file : pickedFiles) {
            if (file.node == node) {
                return true;
            }
        }
        return false;
    }
}
