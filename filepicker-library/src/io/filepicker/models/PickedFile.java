package io.filepicker.models;

import java.util.ArrayList;

/**
 * Created by maciejwitowski on 10/30/14.
 */
public final class PickedFile {
    public Node node;
    public final int position;

    public PickedFile(Node node, int position) {
        this.node = node;
        this.position = position;
    }

    public static ArrayList<Node> getNodes(ArrayList<PickedFile> pickedFiles) {
        ArrayList<Node> nodes = new ArrayList<>(pickedFiles.size());
        for(PickedFile file : pickedFiles) {
            nodes.add(file.node);
        }

        return nodes;
    }

    public static void removeAtPosition( ArrayList<PickedFile> pickedFiles, int position) {
        for(PickedFile file : pickedFiles) {
            if(file.position == position) {
                pickedFiles.remove(file);
                break;
            }
        }
    }

    public static boolean containsPosition(ArrayList<PickedFile> pickedFiles, int position) {
        for(PickedFile file : pickedFiles) {
            if (file.position == position){
                return true;
            }
        }

        return false;
    }
}
