package frc.robot.subsystems;

import com.ctre.phoenix.sensors.Pigeon2.AxisDirection;
import com.ctre.phoenix.sensors.WPI_Pigeon2;
import edu.wpi.first.math.estimator.SwerveDrivePoseEstimator;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.geometry.Twist2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModulePosition;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.CommandBase;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.*;
import frc.robot.util.GeometryUtils;

import java.util.Optional;

public class Swerve extends SubsystemBase {
  public SwerveDrivePoseEstimator swerveOdometry;
  public SwerveModule[] mSwerveMods;
  public WPI_Pigeon2 gyro;

  public Vision vision = new Vision();

  public Swerve() {
    gyro = new WPI_Pigeon2(Constants.Swerve.pigeonID);
    gyro.configFactoryDefault();
    gyro.configMountPose(AxisDirection.NegativeY, AxisDirection.PositiveZ);
    zeroGyro();

    mSwerveMods =
        new SwerveModule[] {
          new SwerveModule(0, Constants.Swerve.Mod0.constants),
          new SwerveModule(1, Constants.Swerve.Mod1.constants),
          new SwerveModule(2, Constants.Swerve.Mod2.constants),
          new SwerveModule(3, Constants.Swerve.Mod3.constants)
        };

    /* By pausing init for a second before setting module offsets, we avoid a bug with inverting motors.
     * See https://github.com/Team364/BaseFalconSwerve/issues/8 for more info.
     */
    Timer.delay(1.0);
    resetModulesToAbsolute();

    // Class for swerve drive odometry. Odometry allows you to track the robot's position on the
    // field
    // over a course of a match using readings from your swerve drive encoders and swerve azimuth
    // encoders.
    swerveOdometry =
        new SwerveDrivePoseEstimator(
            Constants.Swerve.swerveKinematics, getYaw(), getModulePositions(), new Pose2d());
  }
  private static ChassisSpeeds correctForDynamics(ChassisSpeeds originalSpeeds) {
    final double LOOP_TIME_S = 0.02;
    Pose2d futureRobotPose =
        new Pose2d(
            originalSpeeds.vxMetersPerSecond * LOOP_TIME_S,
            originalSpeeds.vyMetersPerSecond * LOOP_TIME_S,
            Rotation2d.fromRadians(originalSpeeds.omegaRadiansPerSecond * LOOP_TIME_S));
    Twist2d twistForPose = GeometryUtils.log(futureRobotPose);
    ChassisSpeeds updatedSpeeds =
        new ChassisSpeeds(
            twistForPose.dx / LOOP_TIME_S,
            twistForPose.dy / LOOP_TIME_S,
            twistForPose.dtheta / LOOP_TIME_S);
    return updatedSpeeds;
}


  public void drive(
      Translation2d translation, double rotation, boolean fieldRelative, boolean isOpenLoop) {
    var speeds = new ChassisSpeeds(translation.getX(), translation.getY(), rotation);
    // fieldRelative: When true, the positional inputs are oriented to the field.
    // Pressing up moves the robot up relative to the field, regardless of the robot's rotation.
    // When false, think of it like strafing. Pressing up will move the robot forward,
    // and pressing left and right will move the robot side to side (relative to itself).
    // In teleop, this is controlled by a button that is held down.
    // Note that the robot does not have a concept of the field,
    // and the gyro must be configured according to starting orientation.
    speeds = correctForDynamics(speeds); //jimmy p omegabytes
    var robotRelative =
        fieldRelative
            ? ChassisSpeeds.fromFieldRelativeSpeeds(speeds, getPose().getRotation())
            : speeds;
    SwerveModuleState[] swerveModuleStates =
        Constants.Swerve.swerveKinematics.toSwerveModuleStates(robotRelative);
    // Normalizes wheel speeds by the max (wheel) speed.
    SwerveDriveKinematics.desaturateWheelSpeeds(
        swerveModuleStates,
        robotRelative,
        Constants.Swerve.maxSpeed,
        Constants.Swerve.maxSpeed,
        Constants.Swerve.maxAngularVelocity);

    for (SwerveModule mod : mSwerveMods) {
      mod.setDesiredState(swerveModuleStates[mod.moduleNumber], isOpenLoop);
    }
  }

  /* Used by SwerveControllerCommand in Auto, instead of the drive method above, which is used by teleop */
  public void setModuleStates(SwerveModuleState[] desiredStates) {
    SwerveDriveKinematics.desaturateWheelSpeeds(desiredStates, Constants.Swerve.maxSpeed);

    for (SwerveModule mod : mSwerveMods) {
      mod.setDesiredState(desiredStates[mod.moduleNumber], false);
    }
  }

  public void lockModules() {
    for (SwerveModule mod : mSwerveMods) {
      mod.setDesiredState(
          new SwerveModuleState(0, new Rotation2d(90 * Math.ceil((mod.moduleNumber % 3) / 2) - 45)),
          true);
    }
  }

  public void lockModules(Boolean input) {
    lockModules(); // Make lockModules possible to use in FunctionalCommands
  }

  public Command lockModulesCommand() {
    return run(this::lockModules);
  }

  public Pose2d getPose() {
    return swerveOdometry.getEstimatedPosition();
  }

  public void resetOdometry(Pose2d pose) {
    swerveOdometry.resetPosition(getYaw(), getModulePositions(), pose);
  }

  public SwerveModuleState[] getModuleStates() {
    SwerveModuleState[] states = new SwerveModuleState[4];
    for (SwerveModule mod : mSwerveMods) {
      states[mod.moduleNumber] = mod.getState();
    }
    return states;
  }

  public SwerveModulePosition[] getModulePositions() {
    SwerveModulePosition[] positions = new SwerveModulePosition[4];
    for (SwerveModule mod : mSwerveMods) {
      positions[mod.moduleNumber] = mod.getPosition();
    }
    return positions;
  }

  public void zeroGyro() {
    gyro.setYaw(0);
  }

  public void gyroFlip180() {
    gyro.setYaw(gyro.getYaw() + 180);
  }

  public Rotation2d getYaw() {
    return gyro.getRotation2d();
  }

  public void resetModulesToAbsolute() {
    for (SwerveModule mod : mSwerveMods) {
      mod.resetToAbsolute();
    }
  }

  public CommandBase resetModulesToAbsoluteCommand() {
    return runOnce(this::resetModulesToAbsolute);
  }

  public double[] getGravity() {
    double[] grav = new double[3];
    gyro.getGravityVector(grav);
    return grav;
  }

  public double getTiltMagnitude() {
    double[] grav = getGravity();
    return Math.sqrt(grav[0] * grav[0] + grav[1] * grav[1]);
  }

  public Rotation2d getTiltDirection() {
    double[] grav = getGravity();
    return new Rotation2d(grav[0], grav[1]);
  }

  double previousRadians;

  /**
   * Returns velocity in radians per second. This is intended to be used for dashboard graphing,
   * etc. not anything that relies on accuracy
   */
  private double getBadAngularVelocityEstimate(Rotation2d yaw) {
    double currentRadians = yaw.getRadians();
    previousRadians = currentRadians;

    double velocity = (currentRadians - previousRadians) * (1.0 / Robot.kDefaultPeriod);
    return velocity;
  }

  double[] previousDistances = new double[] {0, 0, 0, 0};

  @Override
  public void periodic() {
    Rotation2d yaw = getYaw();
    SwerveModulePosition[] poses = getModulePositions();
    swerveOdometry.update(yaw, poses);

    int modNumber = 0;
    for (SwerveModulePosition p : poses) {
      SwerveModule mod = mSwerveMods[modNumber];
      double dist = p.distanceMeters;
      SmartDashboard.putNumber("Mod " + modNumber + " Cancoder", mod.getCanCoder().getDegrees());
      SmartDashboard.putNumber(
          /* Bad estimate, only for graphing/etc */
          "Mod " + modNumber + " Est. Velocity",
          (dist - previousDistances[modNumber]) * (1.0 / Robot.kDefaultPeriod));
      SmartDashboard.putNumber("Mod " + modNumber + " distance", dist);
      SmartDashboard.putNumber("Mod " + modNumber + " Angle", p.angle.getDegrees());
      SmartDashboard.putNumber("Mod " + modNumber + " Drive current", mod.getDriveCurrent());
      previousDistances[modNumber] = dist;
      modNumber++;
    }

    Optional<Vision.PoseEstimate> poseEst = vision.getEstimatedPose();
    if (poseEst.isPresent()) {
      Vision.PoseEstimate est = poseEst.get();
      swerveOdometry.addVisionMeasurement(est.estimatedPose, est.timestampSeconds);
    }

    SmartDashboard.putNumber("Turning velocity", getBadAngularVelocityEstimate(yaw));

    SmartDashboard.putNumber("Gyro yaw", yaw.getDegrees());

    Pose2d pose = getPose();
    SmartDashboard.putNumber("Robot X", pose.getX());
    SmartDashboard.putNumber("Robot Y", pose.getY());
  }
}
