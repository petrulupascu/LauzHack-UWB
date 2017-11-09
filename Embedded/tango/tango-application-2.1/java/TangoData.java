package ch.hefr.etu.zoutao_wen.tangoapplication;

/**
 * Describe the Tango Data that transmit via wifi
 */

class TangoData {
    private final static String TAG = TangoData.class.getSimpleName();

    private double mTimeStamp;
    private String mTagID;
    private String mAdfUUID;
    private float mPosX;
    private float mPosY;
    private float mPosZ;
    private float mThetaX;
    private float mThetaY;
    private float mThetaZ;

    public TangoData(double mTimeStamps, String mTagID, String mAdfUUID, float mPosX, float mPosY,
                     float mPosZ, float mThetaX, float mThetaY, float mThetaZ) {
        this.mTimeStamp = mTimeStamps;
        this.mTagID = mTagID;
        this.mAdfUUID = mAdfUUID;
        this.mPosX = mPosX;
        this.mPosY = mPosY;
        this.mPosZ = mPosZ;
        this.mThetaX = mThetaX;
        this.mThetaY = mThetaY;
        this.mThetaZ = mThetaZ;
    }

    public String getMsg(){
        return "{\"Time_Stamps\":\"" + mTimeStamp +
                "\",\"Tag_ID\":\"" + mTagID +
                "\",\"ADF_UUID\":\"" + mAdfUUID +
                "\",\"POS_X\":\"" + mPosX +
                "\",\"POS_Y\":\"" + mPosY +
                "\",\"POS_Z\":\"" + mPosZ +
                "\",\"THETA_X\":\"" + mThetaX +
                "\",\"THETA_Y\":\"" + mThetaY +
                "\",\"THETA_Z\":\"" + mThetaZ + "\"}\n";
    }
}
