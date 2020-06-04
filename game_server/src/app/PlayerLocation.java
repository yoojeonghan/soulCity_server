package app;

public class PlayerLocation {
	public String PlayerName = null;
	public float LocationX = 0.0f;
	public float LocationY = 0.0f;
	public float LocationZ = 0.0f;
	public float RotationPitch = 0.0f;
	public float RotationRoll = 0.0f;
	public float RotationYaw = 0.0f;

	PlayerLocation(String playerName, float X, float Y, float Z, float P, float R, float Ya) {
		PlayerName = playerName;
		LocationX = X;
		LocationY = Y;
		LocationZ = Z;
		RotationPitch = P;
		RotationRoll = R;
		RotationYaw = Ya;
	}
}
