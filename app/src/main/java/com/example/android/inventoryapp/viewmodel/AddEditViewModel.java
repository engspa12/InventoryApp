package com.example.android.inventoryapp.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.example.android.inventoryapp.AppDatabase;
import com.example.android.inventoryapp.ProductItem;

public class AddEditViewModel extends ViewModel {

    private LiveData<ProductItem> productItemLiveData;

    public AddEditViewModel(AppDatabase database, int id){
        productItemLiveData = database.productItemDao().loadProductById(id);
    }

    public LiveData<ProductItem> getProductItemLiveData(){
        return productItemLiveData;
    }
}
