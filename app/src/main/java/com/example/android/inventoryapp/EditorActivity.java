package com.example.android.inventoryapp;

import android.arch.lifecycle.ViewModelProvider;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.media.ExifInterface;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.Toast;

import com.example.android.inventoryapp.data.InventoryContract.InventoryEntry;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.IOException;
import java.io.InputStream;

import butterknife.BindView;
import butterknife.ButterKnife;

public class EditorActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    @BindView(R.id.product_type_spinner) Spinner productTypesSpinner;
    @BindView(R.id.in_stock_spinner) Spinner inStockSpinner;
    @BindView(R.id.product_image) ImageView productImageView;

    @BindView(R.id.details_button) Button multi_button;
    @BindView(R.id.quantity_minus) Button decreaseQuantityButton;
    @BindView(R.id.quantity_plus) Button increaseQuantityButton;
    @BindView(R.id.order_supplier_button) Button orderFromSupplierButton;
    @BindView(R.id.increase_quantity_by_button) Button increaseQuantityByButton;
    @BindView(R.id.decrease_quantity_by_button) Button decreaseQuantityByButton;

    @BindView(R.id.product_brand) EditText brandET;
    @BindView(R.id.product_warranty) EditText warrantyET;
    @BindView(R.id.product_year_manufacture) EditText dateManufactureET;
    @BindView(R.id.product_weight) EditText weightET;
    @BindView(R.id.product_price) EditText priceET;
    @BindView(R.id.product_quantity) EditText quantityET;
    @BindView(R.id.product_name) EditText nameET ;
    @BindView(R.id.modify_quantity_by_et) EditText modifyQuantityET;

    @BindView(R.id.increase_decrease_order__buttons_section) LinearLayout containerOne;
    @BindView(R.id.increase_decrease_section) LinearLayout containerTwo;

    @BindView(R.id.progress_bar_save_item) ProgressBar progressBarSaveItem;

    @BindView(R.id.scroll_view_detail) ScrollView scrollView;

    private static final int RC_PHOTO_PICKER = 2;

    private static final int NEW_IMAGE_CODE = 123;

    private Uri wantedUri;
    private int typeProduct;
    private int inStock;
    private int image_id_resource;


    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference productsDatabaseReference;

    private FirebaseStorage firebaseStorage;
    private StorageReference productsStorageReference;

    private FirebaseAuth mAuth;
    private FirebaseAuth.AuthStateListener mAuthListener;

    private Uri selectedImageUri;
    private String downloadImageUrl;

    private static final String TAG = EditorActivity.class.getSimpleName();

    private String brand;
    private int warrantyInt;
    private String yearManufacture;
    private double weightDouble;
    private double priceDouble;
    private int quantityInt;
    private String name;

    private Bitmap selectedImageBitmap;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.detail_layout);

        ButterKnife.bind(this);

        wantedUri = getIntent().getData();

        firebaseDatabase = FirebaseDatabase.getInstance();
        productsDatabaseReference = firebaseDatabase.getReference().child("products");

        mAuth = FirebaseAuth.getInstance();
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }

            }
        };

        if (wantedUri == null) {
            // This is a new product, so change the app bar to say "Add a Product"
            setTitle("Add Product");
            image_id_resource = R.drawable.select_image;
            productImageView.setImageResource(image_id_resource);
            image_id_resource = -1;
            // Invalidate the options menu, so the "Delete" menu option can be hidden.
            // (It doesn't make sense to delete a product that hasn't been created yet.)
            multi_button.setText(getString(R.string.select_image_button));
            decreaseQuantityButton.setVisibility(View.GONE);
            increaseQuantityButton.setVisibility(View.GONE);
            orderFromSupplierButton.setVisibility(View.GONE);
            increaseQuantityByButton.setVisibility(View.GONE);
            decreaseQuantityByButton.setVisibility(View.GONE);

            firebaseStorage = FirebaseStorage.getInstance();
            productsStorageReference = firebaseStorage.getReference().child("inventory_photos");

            multi_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // TODO: Fire an intent to show an image picker
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.setType("image/jpeg");
                    intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                    startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
                }
            });

            modifyQuantityET.setVisibility(View.GONE);

            containerOne.setVisibility(View.GONE);
            containerTwo.setVisibility(View.GONE);
            invalidateOptionsMenu();

            progressBarSaveItem.setVisibility(View.GONE);


        } else {
            // Otherwise this is an existing product, so change app bar to say "Edit Product"
            setTitle("Product Details");
            scrollView.setVisibility(View.GONE);
            multi_button.setText(getString(R.string.delete_product_button));
            avoidModifications();
            invalidateOptionsMenu();

            // Initialize a loader to read the product data from the database
            // and display the current values in the editor
            getSupportLoaderManager().initLoader(0, null, this);

            multi_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDeleteConfirmationDialog();
                }
            });

            decreaseQuantityButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int quantity = Integer.parseInt(quantityET.getText().toString().trim());
                    if (quantity > 0) {
                        quantity--;
                        quantityET.setText(String.valueOf(quantity));
                        Toast.makeText(getBaseContext(), "Quantity value is: " + quantity, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getBaseContext(), "The min quantity allowed is 0", Toast.LENGTH_SHORT).show();
                    }
                }
            });


            increaseQuantityButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int quantity = Integer.parseInt(quantityET.getText().toString().trim());
                    if (quantity < 15) {
                        quantity++;
                        quantityET.setText(String.valueOf(quantity));
                        Toast.makeText(getBaseContext(), "Quantity value is: " + quantity, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getBaseContext(), "The max quantity allowed is 15", Toast.LENGTH_SHORT).show();
                    }
                }
            });


            orderFromSupplierButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    int quantity = Integer.parseInt(quantityET.getText().toString().trim());
                    String[] addresses = {"arturo.lpc12@gmail.com"};
                    String brand = brandET.getText().toString().trim();
                    String name = nameET.getText().toString().trim();
                    String price = String.format("%.2f", Double.parseDouble(priceET.getText().toString().trim()));
                    String message = "Dear Provider,\n\n We need "+ quantity + " more items from the product " +
                            "with the following data:\n\nName: " + name + "\nBrand: " + brand + "\nPrice: USD " + price +
                            "\n\nThank you for your attention.";
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:")); // only email apps should handle this
                    intent.putExtra(Intent.EXTRA_EMAIL, addresses);
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Sale Order");
                    intent.putExtra(Intent.EXTRA_TEXT,message);
                    if (intent.resolveActivity(getPackageManager()) != null) {
                        startActivity(intent);
                    }
                }
            });


            increaseQuantityByButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String amountString = modifyQuantityET.getText().toString().trim();
                    int amount;
                    if (amountString.equals("")) {
                        amount = 0;
                        Toast.makeText(getBaseContext(), "You need to enter a quantity in the input field", Toast.LENGTH_SHORT).show();
                    } else {
                        amount = Integer.parseInt(amountString);
                        int quantity = Integer.parseInt(quantityET.getText().toString().trim());
                        quantity = quantity + amount;
                        if (quantity > 15) {
                            Toast.makeText(getBaseContext(), "The max quantity allowed is 15", Toast.LENGTH_SHORT).show();
                        } else {
                            quantityET.setText(String.valueOf(quantity));
                            Toast.makeText(getBaseContext(), "Quantity value is: " + quantity, Toast.LENGTH_SHORT).show();
                        }
                    }


                }
            });

            decreaseQuantityByButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String amountString = modifyQuantityET.getText().toString().trim();
                    int amount;
                    if (amountString.equals("")) {
                        amount = 0;
                        Toast.makeText(getBaseContext(), "You need to enter a quantity in the input field", Toast.LENGTH_SHORT).show();
                    } else {
                        amount = Integer.parseInt(amountString);
                        int quantity = Integer.parseInt(quantityET.getText().toString().trim());
                        quantity = quantity - amount;
                        if (quantity < 0) {
                            Toast.makeText(getBaseContext(), "The min quantity allowed is 0", Toast.LENGTH_SHORT).show();
                        } else {
                            quantityET.setText(String.valueOf(quantity));
                            Toast.makeText(getBaseContext(), "Quantity value is: " + quantity, Toast.LENGTH_SHORT).show();
                        }
                    }


                }
            });


        }

        setupSpinners();


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_PHOTO_PICKER && resultCode == RESULT_OK) {
            selectedImageUri = data.getData();
            selectedImageBitmap = null;
            try {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    Bitmap capturedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                    int origBitmapSize = capturedBitmap.getAllocationByteCount();
                    selectedImageBitmap = Bitmap.createScaledBitmap(capturedBitmap, capturedBitmap.getWidth() / 3, capturedBitmap.getHeight() / 3, true);
                    int newBitmapSize = selectedImageBitmap.getAllocationByteCount();
                    Log.v(TAG,"Old value of Bitmap: " + String.valueOf(origBitmapSize));
                    Log.v(TAG,"New size of Bitmap: " + String.valueOf(newBitmapSize));
                }

            } catch(IOException e){
                Log.e(TAG,e.getMessage());
            }
            //Toast.makeText(this,selectedImageUri.toString(),Toast.LENGTH_LONG).show();
            //productImageView.setImageURI(selectedImageUri);
            productImageView.setImageBitmap(selectedImageBitmap);
            //Picasso.get().load(selectedImageUri).into(productImageView);
            //checkImageRotation(data);
            //String path = FileUtility.getRealPathFromURI(this, selectedImageUri);

           /* try {
                ExifInterface exif = new ExifInterface(path);
                int rotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                Toast.makeText(this,rotation,Toast.LENGTH_LONG).show();
            } catch(IOException e){
                Log.e(LOG,e.getMessage());
            }*/
            image_id_resource = NEW_IMAGE_CODE;
            //saveInStorage();
        }
    }


    private void checkImageRotation(Intent data){
        BitmapFactory.Options options;
        try {
            options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            Uri selectedImage = data.getData();
            String[] filePath = {MediaStore.Images.Media.DATA};

            Cursor cursor = getContentResolver().query(selectedImage, filePath, null, null, null);
            cursor.moveToFirst();
            String mImagePath = cursor.getString(cursor.getColumnIndex(filePath[0]));

            InputStream stream = getContentResolver().openInputStream(selectedImage);
            Bitmap yourSelectedImage = BitmapFactory.decodeStream(stream, null, options);
            stream.close();
            //orientation
            try {
                int rotate = 0;
                try {
                    ExifInterface exif = new ExifInterface(
                            mImagePath);
                    int orientation = exif.getAttributeInt(
                            ExifInterface.TAG_ORIENTATION,
                            ExifInterface.ORIENTATION_NORMAL);

                    switch (orientation) {
                        case ExifInterface.ORIENTATION_ROTATE_270:
                            rotate = 270;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_180:
                            rotate = 180;
                            break;
                        case ExifInterface.ORIENTATION_ROTATE_90:
                            rotate = 90;
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Matrix matrix = new Matrix();
                matrix.postRotate(rotate);
                yourSelectedImage = Bitmap.createBitmap(yourSelectedImage , 0, 0, yourSelectedImage.getWidth(), yourSelectedImage.getHeight(), matrix, true);  }
            catch (Exception e) {}
            //end of orientation

            //productImageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
            productImageView.setImageBitmap(yourSelectedImage);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "Could not open file.", Toast.LENGTH_LONG).show();
        }
    }

    private void saveInStorage(){
        final StorageReference photoRef =
               productsStorageReference.child(selectedImageUri.getLastPathSegment());

        UploadTask uploadTask = photoRef.putFile(selectedImageUri);
        uploadTask.addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                Toast.makeText(EditorActivity.this,"The file was uploaded successfully",Toast.LENGTH_SHORT).show();
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(EditorActivity.this,"Error - The file could not be uploaded",Toast.LENGTH_SHORT).show();
                Log.e(TAG,e.getMessage());
            }
        });

        uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
            @Override
            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                if (!task.isSuccessful()) {
                    throw task.getException();
                }

                // Continue with the task to get the download URL
                return photoRef.getDownloadUrl();
            }
        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
            @Override
            public void onComplete(@NonNull Task<Uri> task) {
                if (task.isSuccessful()) {
                    Uri imageDownloadUri = task.getResult();
                    downloadImageUrl = imageDownloadUri.toString();
                    progressBarSaveItem.setVisibility(View.GONE);
                    saveProduct();
                    scrollView.setVisibility(View.VISIBLE);
                    finish();
                } else {
                    // Handle failures
                    Toast.makeText(EditorActivity.this,"The size of the image must be below 3 MB",Toast.LENGTH_LONG).show();
                    progressBarSaveItem.setVisibility(View.GONE);
                    scrollView.setVisibility(View.VISIBLE);
                    Log.e(TAG,task.getException().getMessage());
                }
            }
        });
    }


    private void avoidModifications() {
        brandET.setKeyListener(null);
        brandET.setBackgroundDrawable(null);
        warrantyET.setKeyListener(null);
        warrantyET.setBackgroundDrawable(null);
        dateManufactureET.setKeyListener(null);
        dateManufactureET.setBackgroundDrawable(null);
        weightET.setKeyListener(null);
        weightET.setBackgroundDrawable(null);
        priceET.setKeyListener(null);
        priceET.setBackgroundDrawable(null);
        quantityET.setKeyListener(null);
        quantityET.setBackgroundDrawable(null);
        nameET.setKeyListener(null);
        nameET.setBackgroundDrawable(null);

    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }

    private void setupSpinners() {

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> typesAdapter = ArrayAdapter.createFromResource(this,
                R.array.product_types_options, R.layout.spinner_item);
        // Specify the layout to use when the list of choices appears
        typesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        productTypesSpinner.setAdapter(typesAdapter);

        productTypesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {

                String selection = (String) adapterView.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.sports_type))) {
                        //Sports Type
                        typeProduct = 0;
                    } else if (selection.equals(getString(R.string.technology_type))) {
                        //Technology Type
                        typeProduct = 1;
                    } else if (selection.equals(getString(R.string.furniture_type))) {
                        //Furniture Type
                        typeProduct = 2;
                    } else if (selection.equals(getString(R.string.other_type))) {
                        //Clothing Type
                        typeProduct = 3;
                    } else{
                        //Other Type
                        typeProduct = 4;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                typeProduct = 0; //Sports is the default choice
            }
        });

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> inStockAdapter = ArrayAdapter.createFromResource(this,
                R.array.in_stock_options, R.layout.spinner_item);
        // Specify the layout to use when the list of choices appears
        inStockAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        inStockSpinner.setAdapter(inStockAdapter);

        inStockSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {

                String selection = (String) adapterView.getItemAtPosition(position);
                if (!TextUtils.isEmpty(selection)) {
                    if (selection.equals(getString(R.string.not_in_stock))) {
                        //Product is not in Stock
                        inStock = 0;
                    } else if (selection.equals(getString(R.string.in_stock))) {
                        //Product in Stock
                        inStock = 1;
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                inStock = 1; //In Stock is the default choice
            }
        });


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editor_add, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        // If this is a new item, hide the "Delete" menu item.
        //if (wantedUri == null) {
        MenuItem menuItem = menu.findItem(R.id.action_delete);
        menuItem.setVisible(false);
        // }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // User clicked on a menu option in the app bar overflow menu
        switch (item.getItemId()) {
            // Respond to a click on the "Insert dummy data" menu option
            case R.id.action_save:
                if(checkInputData()) {
                    progressBarSaveItem.setVisibility(View.VISIBLE);
                    scrollView.setVisibility(View.GONE);
                    saveInStorage();
                }
                return true;
            // Respond to a click on the "Delete all entries" menu option
            //case R.id.action_delete:
               // showDeleteConfirmationDialog();
                //deleteProduct();
             //   return true;
        }
        return super.onOptionsItemSelected(item);

    }

    private void showDeleteConfirmationDialog() {
        // Create an AlertDialog.Builder and set the message, and click listeners
        // for the positive and negative buttons on the dialog.
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.delete_dialog_msg);
        builder.setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Delete" button, so delete the item.
                deleteProduct();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked the "Cancel" button, so dismiss the dialog
                // and continue editing the item.
                if (dialog != null) {
                    dialog.dismiss();
                }
            }
        });

        // Create and show the AlertDialog
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    public boolean checkInputData(){

        brand = brandET.getText().toString().trim();
        String warranty = warrantyET.getText().toString().trim();
        yearManufacture = dateManufactureET.getText().toString().trim();
        String weight = weightET.getText().toString().trim();
        String price = priceET.getText().toString().trim();
        String quantity = quantityET.getText().toString().trim();
        name = nameET.getText().toString().trim();

        if (brand.equals("") || warranty.equals("") || yearManufacture.equals("") || weight.equals("") ||
                price.equals("") || quantity.equals("") || name.equals("")) {
            Toast.makeText(getBaseContext(), "Null values are not allowed", Toast.LENGTH_SHORT).show();
            return false;
        }


        warrantyInt = Integer.parseInt(warranty);
        weightDouble = Double.parseDouble(weight);
        priceDouble = Double.parseDouble(price);
        quantityInt = Integer.parseInt(quantity);

        if (quantityInt > 15 || quantityInt < 0) {
            Toast.makeText(this, "Quantity must be between 0 and 15", Toast.LENGTH_SHORT).show();
            return false;
        }


        if(image_id_resource == -1 || image_id_resource != NEW_IMAGE_CODE){
            Toast.makeText(this, "You need to select an image from gallery", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    public void saveProduct() {

        ContentValues values = new ContentValues();

        values.put(InventoryEntry.COLUMN_PRODUCT_BRAND, brand);
        values.put(InventoryEntry.COLUMN_PRODUCT_WARRANTY, warrantyInt);
        values.put(InventoryEntry.COLUMN_PRODUCT_YEAR_MANUFACTURE, yearManufacture);
        values.put(InventoryEntry.COLUMN_PRODUCT_WEIGHT, weightDouble);
        values.put(InventoryEntry.COLUMN_PRODUCT_PRICE, priceDouble);
        values.put(InventoryEntry.COLUMN_PRODUCT_QUANTITY, quantityInt);
        values.put(InventoryEntry.COLUMN_PRODUCT_STOCK, inStock);
        values.put(InventoryEntry.COLUMN_PRODUCT_NAME, name);
        values.put(InventoryEntry.COLUMN_PRODUCT_TYPE, typeProduct);
        values.put(InventoryEntry.COLUMN_PRODUCT_IMAGE_URL, downloadImageUrl);

        ProductItem productItem = new ProductItem(brand,warrantyInt,yearManufacture,weightDouble,priceDouble,quantityInt,inStock,name,typeProduct,downloadImageUrl);
        /*productsDatabaseReference.push().setValue(productItem);*/
        productsDatabaseReference.child(name + yearManufacture + brand).setValue(productItem);

        if (wantedUri == null) {
            getContentResolver().insert(InventoryEntry.CONTENT_URI, values);
        } else {
            getContentResolver().update(wantedUri, values, null, null);
        }

    }

    public void deleteProduct() {

        if (wantedUri != null) {
            getContentResolver().delete(wantedUri, null, null);
        }

        finish();
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getBaseContext(), wantedUri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {

        if (data == null || data.getCount() < 1) {
            return;
        }


        if (data.moveToFirst()) {

            String currentBrand = data.getString(data.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_BRAND));
            int currentWarranty = data.getInt(data.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_WARRANTY));
            String currentYearManufacture = data.getString(data.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_YEAR_MANUFACTURE));
            double currentWeight = data.getInt(data.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_WEIGHT));
            double currentPrice = data.getDouble(data.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_PRICE));
            int currentQuantity = data.getInt(data.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_QUANTITY));
            String currentName = data.getString(data.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_NAME));

            int currentType = data.getInt(data.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_TYPE));
            int currentInStock = data.getInt(data.getColumnIndex(InventoryEntry.COLUMN_PRODUCT_STOCK));

            String currentImageResource = data.getString(data.getColumnIndexOrThrow(InventoryEntry.COLUMN_PRODUCT_IMAGE_URL));
            //image_id_resource = currentImageResource;

            brandET.setText(currentBrand);
            warrantyET.setText(String.valueOf(currentWarranty));
            dateManufactureET.setText(currentYearManufacture);
            weightET.setText(String.valueOf(currentWeight));
            priceET.setText(String.valueOf(currentPrice));
            quantityET.setText(String.valueOf(currentQuantity));
            nameET.setText(currentName);

            //productImageView.setImageResource(currentImageResource);
            Picasso.get().load(currentImageResource).resize(250,250).into(productImageView);

            if (currentType == InventoryEntry.SPORTS_TYPE) {
                productTypesSpinner.setSelection(0);
            } else if (currentType == InventoryEntry.TECHNOLOGY_TYPE) {
                productTypesSpinner.setSelection(1);
            } else if (currentType == InventoryEntry.FURNITURE_TYPE) {
                productTypesSpinner.setSelection(2);
            } else if (currentType == InventoryEntry.CLOTHING_TYPE) {
                productTypesSpinner.setSelection(3);
            } else if(currentType == InventoryEntry.OTHER_TYPE){
                productTypesSpinner.setSelection(4);
            }


            if (currentInStock == InventoryEntry.NO_STOCK_AVAILABLE) {
                inStockSpinner.setSelection(0);
            } else if (currentInStock == InventoryEntry.IN_STOCK) {
                inStockSpinner.setSelection(1);
            }

            productTypesSpinner.setEnabled(false);
            inStockSpinner.setEnabled(false);

            progressBarSaveItem.setVisibility(View.GONE);
            scrollView.setVisibility(View.VISIBLE);


        }


    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        brandET.setText("");
        warrantyET.setText("");
        dateManufactureET.setText("");
        weightET.setText("");
        priceET.setText("");
        quantityET.setText("");
        nameET.setText("");
        productTypesSpinner.setSelection(0);
        inStockSpinner.setSelection(0);

    }
}

