package io.filepicker.models;

import java.util.ArrayList;

/**
 * Created by maciejwitowski on 10/30/14.
 */
public class PickedFile {
    Node node;
    int position;

    public PickedFile(Node node, int position) {
        this.node = node;
        this.position = position;
    }

    public Node getNode() {
        return node;
    }

    public int getPosition() {
        return position;
    }

    public static ArrayList<Node> getNodes(ArrayList<PickedFile> pickedFiles) {
        ArrayList<Node> nodes = new ArrayList<Node>(pickedFiles.size());
        for(PickedFile file : pickedFiles) {
            nodes.add(file.getNode());
        }

        return nodes;
    }

    public static void removeAtPosition( ArrayList<PickedFile> pickedFiles, int position) {
        for(PickedFile file : pickedFiles) {
            if(file.getPosition() == position) {
                pickedFiles.remove(file);
                break;
            }
        }
    }

    public static boolean containsPosition(ArrayList<PickedFile> pickedFiles, int position) {
        for(PickedFile file : pickedFiles) {
            if (file.getPosition() == position){
                return true;
            }
        }

        return false;
    }
}
