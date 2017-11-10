
# Socket port and baudrate are also used as systemID
APRIL_PORT = 7580
PIXLIVE_PORT = 7581
BEACON_PORT = 7582
QUUPPA_PORT = 7583
WIFI_PORT = 7584
TANGO_PORT = 7585
SENSORS_PORT = 7586
POZYX_BAUDRATE = 115200

data_fields = [
    "timestamp",        # time in milliseconds in epoch time
    "device_id",        # the device we want to locate (previously named tag_id)
    "system_id",        # we use the UDP port as system id (for now at least, but we must change it to locate several device at a time)
    "anchor_id",        # anchor used to take the current measure (previously named locator_id)
    "px",               # [px, py, pz] is the position of the device in world coordinate
    "py",
    "pz",
    "theta_x",          # [theta_x, theta_y, theta_z] corresponds to the orientation of the device (radian)
    "theta_y",
    "theta_z",
    "dx",               # [dx, dy, dz] corresponds to the DIRECTION UNIT VECTOR from anchor to device
    "dy",
    "dz",
    "distance",         # distance between anchor and device
    "dist_error",       # estimated distance error
    "dist_var",         # estimated distance variance
    "txpower",          # transmitting power of the anchor
    "rssi",             # signal strength received by the device
    "ble_channel",      # Advertising channel (can be 0, 1 or 2) corresponding to channel 37, 38 and 39
    "calib_param_tx",   # [calib_param_tx calib_param_n] are the calibration parameters used to compute the distance
    "calib_param_n"
]

# These are the index of the values when we write them
data_timestamp  = data_fields.index("timestamp")
data_device_id  = data_fields.index("device_id")
data_system_id  = data_fields.index("system_id")
data_anchor_id  = data_fields.index("anchor_id")
data_dx         = data_fields.index("dx")
data_dy         = data_fields.index("dy")
data_dz         = data_fields.index("dz")
data_distance   = data_fields.index("distance")
data_dist_error = data_fields.index("dist_error")
data_dist_var   = data_fields.index("dist_var")
data_txpower    = data_fields.index("txpower")
data_rssi       = data_fields.index("rssi")
data_theta_x    = data_fields.index("theta_x")
data_theta_y    = data_fields.index("theta_y")
data_theta_z    = data_fields.index("theta_z")
data_px         = data_fields.index("px")
data_py         = data_fields.index("py")
data_pz         = data_fields.index("pz")


def data_dict2list(dic):
    return [dic[f] if f in list(dic.keys()) else 'NaN' for f in data_fields]


def data_list2dict(array):
    dic = {}
    for k in range(len(array)):
        val = array[k]
        if val is not None and not val == 'NaN':
            field = data_fields[k]
            dic[field] = val
    return dic


anchor_fields = [
    "system_id",    # we use the UDP port as system id (for now at least, but we must change it to locate several device at a time)
    "anchor_id",    # id of the anchor
    "px",           # [px, py, pz] is the position of the anchor in world coordinate
    "py",
    "pz",
    "theta_x",      # [theta_x, theta_y, theta_z] corresponds to the orientation of the anchor (radian)
    "theta_y",
    "theta_z"
]

# These are the index of the values when we write them
anchor_system_id = anchor_fields.index("system_id")
anchor_anchor_id = anchor_fields.index("anchor_id")
anchor_px = anchor_fields.index("px")
anchor_py = anchor_fields.index("py")
anchor_pz = anchor_fields.index("pz")
anchor_theta_x = anchor_fields.index("theta_x")
anchor_theta_y = anchor_fields.index("theta_y")
anchor_theta_z = anchor_fields.index("theta_z")


def anchor_dict2list(dic):
    return [dic[f] if f in list(dic.keys()) else 'NaN' for f in anchor_fields]


def anchor_list2dict(array):
    dic = {}
    for k in range(len(array)):
        val = array[k]
        if val is not None and not val == 'NaN':
            field = anchor_fields[k]
            dic[field] = val
    return dic


calibration_fields = [
            "timestamp",
            "source_id",
            "measured_id",
            "ble_channel",
            "rssi"
]

def calib_dict2list(dic):
    return [dic[f] if f in list(dic.keys()) else 'NaN' for f in calibration_fields]
