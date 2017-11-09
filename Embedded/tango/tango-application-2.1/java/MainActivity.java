package ch.hefr.etu.zoutao_wen.tangoapplication;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.atap.tangoservice.Tango;

/**
 * This class is the contents of the application.
 * First, the user should explore the environment. Then, he/she can locate in this environment and
 * see more information. The user also can manage the ADF file.
 */

public class MainActivity extends AppCompatActivity {
    // Permission request action.
    public static final int REQUEST_CODE_TANGO_PERMISSION = 0;
    public static final String ACTIVITY =
            "ch.hefr.etu.zoutao_wen.tangoapplication.getactivity";

    //UI elements
    private Button mExploreButton;
    private Button mLocateButton;
    private Button mShowButton;
    private Button mManageButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Set up UI elements
        mExploreButton = (Button) findViewById(R.id.explore);
        mLocateButton = (Button) findViewById(R.id.locate);
        mShowButton = (Button) findViewById(R.id.show);
        mManageButton = (Button) findViewById(R.id.manage_adf);

        startActivityForResult(
                Tango.getRequestPermissionIntent(Tango.PERMISSIONTYPE_ADF_LOAD_SAVE), 0);
    }

    public void exploreClicked(View v){
        Intent startExploreIntent = new Intent(this, ExploreActivity.class);
        startActivity(startExploreIntent);
    }

    public void locateClicked(View v){
        Intent startLocateIntent = new Intent(this, LocateActivity.class);
        startActivity(startLocateIntent);
    }

    public void showClicked(View v){
        //TODO:
        Toast.makeText(this, "Not yet developed", Toast.LENGTH_SHORT).show();
    }

    public void manageAdfClicked(View v){
        Intent startAdfListViewIntent = new Intent(this, AdfUuidListViewActivity.class);
        startAdfListViewIntent.putExtra(ACTIVITY , MainActivity.class.toString());
        startActivity(startAdfListViewIntent);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // The result of the permission activity.
        // Check which request we're responding to.
        if (requestCode == REQUEST_CODE_TANGO_PERMISSION) {
            // Make sure the request was successful.
            if (resultCode == RESULT_CANCELED) {
                Toast.makeText(this, R.string.app_name, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}
