package com.ExtraBossRush.GoM.Support;

public class LU {
    /**
     * (px,py,pz) から (tx,ty,tz) を見るための Yaw と Pitch を返す
     * @return float[]{ yaw, pitch }
     */
    public static float[] calculateLookAt(
            double px, double py, double pz,
            double tx, double ty, double tz
    ) {
        double dx = tx - px;
        double dy = ty - py;
        double dz = tz - pz;
        double distXZ = Math.sqrt(dx * dx + dz * dz);

        //atan2(dz, dx) は X 軸正方向から反時計回りの角度(rad)
        float yaw   = (float)(Math.toDegrees(Math.atan2(dz, dx))) - 90f;
        float pitch = (float)(-Math.toDegrees(Math.atan2(dy, distXZ)));
        return new float[]{ yaw, pitch };
    }
}