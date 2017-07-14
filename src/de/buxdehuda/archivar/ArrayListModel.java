package de.buxdehuda.archivar;

import java.util.List;
import javax.swing.AbstractListModel;

public class ArrayListModel<T> extends AbstractListModel<T> {
    
    private final List<T> backingList;
    
    public ArrayListModel(List<T> list) {
        this.backingList = list;
    }

    @Override
    public int getSize() {
        return backingList.size();
    }

    @Override
    public T getElementAt(int index) {
        return backingList.get(index);
    }
    
}
