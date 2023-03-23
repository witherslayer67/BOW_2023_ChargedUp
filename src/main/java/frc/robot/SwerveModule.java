package frc.robot;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.DemandType;
import com.ctre.phoenix.motorcontrol.can.WPI_TalonFX;
import com.ctre.phoenix.sensors.CANCoder;
import com.ctre.phoenix.sensors.WPI_CANCoder;

import edu.wpi.first.math.controller.SimpleMotorFeedforward;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import frc.lib.math.Conversions;
import frc.lib.util.CTREModuleState;
import frc.lib.util.SwerveModuleConstants;

public class SwerveModule {
  public int moduleNumber;
  private Rotation2d angleOffset;
  private Rotation2d lastAngle;

  private WPI_TalonFX mAngleMotor;
  private WPI_TalonFX mDriveMotor;
  private WPI_CANCoder angleEncoder;

  SimpleMotorFeedforward feedforward =
      new SimpleMotorFeedforward(
          Constants.Swerve.driveKS, Constants.Swerve.driveKV, Constants.Swerve.driveKA);

  public SwerveModule(int moduleNumber, SwerveModuleConstants moduleConstants) {
    this.moduleNumber = moduleNumber;
    this.angleOffset = moduleConstants.angleOffset;

    /* Angle Encoder Config */
    angleEncoder = new WPI_CANCoder(moduleConstants.cancoderID);
    configAngleEncoder();

    /* Angle Motor Config */
    mAngleMotor = new WPI_TalonFX(moduleConstants.angleMotorID);
    configAngleMotor();

    /* Drive Motor Config */
    mDriveMotor = new WPI_TalonFX(moduleConstants.driveMotorID);
    configDriveMotor();

    lastAngle = getState().angle;
  }

  public void setDesiredState(SwerveModuleState desiredState, boolean isOpenLoop) {
    // isOpenLoop is true during teleop and false in auto (uses feedfoward and pid control during
    // auto).
    // Calculates the shortest path to the desired angle, e.x. 340 -> 20 is +40 instead of -320.
    desiredState = CTREModuleState.optimize(desiredState, getState().angle);
    setAngle(desiredState);
    setSpeed(desiredState, isOpenLoop);
  }

  private void setSpeed(SwerveModuleState desiredState, boolean isOpenLoop) {
    if (isOpenLoop) {
      double percentOutput = desiredState.speedMetersPerSecond / Constants.Swerve.maxSpeed;
      mDriveMotor.set(ControlMode.PercentOutput, percentOutput);
    } else {
      double velocity =
          Conversions.MPSToFalcon(
              desiredState.speedMetersPerSecond,
              Constants.Swerve.wheelCircumference,
              Constants.Swerve.driveGearRatio);
      mDriveMotor.set(
          ControlMode.Velocity,
          velocity,
          DemandType.ArbitraryFeedForward,
          feedforward.calculate(desiredState.speedMetersPerSecond));
    }
  }

  private void setAngle(SwerveModuleState desiredState) {
    // 1% control deadband to prevent jittering. Note that this is module angle/direction, not
    // robot.
    // If we aren't moving that much, it doesn't matter that it's not in the correct direction.
    Rotation2d angle =
        /*(Math.abs(desiredState.speedMetersPerSecond) <= (Constants.Swerve.maxSpeed * 0.01))
        ? lastAngle // I decided to comment this out because it might be causing some weird issues
        : */ desiredState.angle;

    mAngleMotor.set(
        ControlMode.Position,
        Conversions.degreesToFalcon(angle.getDegrees(), Constants.Swerve.angleGearRatio));
    lastAngle = angle;
  }

  private Rotation2d getAngle() {
    return Rotation2d.fromDegrees(
        Conversions.falconToDegrees(
            mAngleMotor.getSelectedSensorPosition(), Constants.Swerve.angleGearRatio));
  }

  public Rotation2d getCanCoder() {
    return Rotation2d.fromDegrees(angleEncoder.getAbsolutePosition());
  }

  // Measures the absolute encoder value and offset, and configures the angle motor to know the
  // current angle.
  public void resetToAbsolute() {
    double absolutePosition =
        Conversions.degreesToFalcon(
            getCanCoder().getDegrees() - angleOffset.getDegrees(), Constants.Swerve.angleGearRatio);
    mAngleMotor.setSelectedSensorPosition(absolutePosition);
  }

  private void configAngleEncoder() {
    angleEncoder.configFactoryDefault();
    angleEncoder.configAllSettings(Robot.ctreConfigs.swerveCanCoderConfig);
  }

  private void configAngleMotor() {
    mAngleMotor.configFactoryDefault();
    mAngleMotor.configAllSettings(Robot.ctreConfigs.swerveAngleFXConfig);
    mAngleMotor.setInverted(Constants.Swerve.angleMotorInvert);
    mAngleMotor.setNeutralMode(Constants.Swerve.angleNeutralMode);
    resetToAbsolute();
  }

  private void configDriveMotor() {
    mDriveMotor.configFactoryDefault();
    mDriveMotor.configAllSettings(Robot.ctreConfigs.swerveDriveFXConfig);
    mDriveMotor.setInverted(Constants.Swerve.driveMotorInvert);
    mDriveMotor.setNeutralMode(Constants.Swerve.driveNeutralMode);
    mDriveMotor.setSelectedSensorPosition(0);
  }

  public SwerveModuleState getState() {
    return new SwerveModuleState(
        Conversions.falconToMPS(
            mDriveMotor.getSelectedSensorVelocity(),
            Constants.Swerve.wheelCircumference,
            Constants.Swerve.driveGearRatio),
        getAngle());
  }

  public SwerveModulePosition getPosition() {
    return new SwerveModulePosition(
        Conversions.falconToMeters(
            mDriveMotor.getSelectedSensorPosition(),
            Constants.Swerve.wheelCircumference,
            Constants.Swerve.driveGearRatio),
        getAngle());
  }

  public double getDriveCurrent() {
    return mDriveMotor.getSupplyCurrent();
  }
}
