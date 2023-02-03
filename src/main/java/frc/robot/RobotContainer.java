package frc.robot;

import edu.wpi.first.wpilibj.GenericHID;
import edu.wpi.first.wpilibj.Joystick;
import edu.wpi.first.wpilibj.PS4Controller;
import edu.wpi.first.wpilibj.XboxController;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.button.JoystickButton;

import frc.robot.autos.*;
import frc.robot.commands.*;
import frc.robot.commands.intake.IntakeIn;
import frc.robot.commands.intake.SpInintake;
import frc.robot.commands.intake.SpInintake.IntakeSpinStatus;
import frc.robot.commands.swerve.BalanceThing;
import frc.robot.commands.swerve.GoToPoint;
import frc.robot.commands.swerve.TeleopSwerve;
import frc.robot.subsystems.*;

import org.photonvision.PhotonCamera;
import org.photonvision.PhotonPoseEstimator;
import org.photonvision.PhotonPoseEstimator.PoseStrategy;
import org.photonvision.common.hardware.VisionLEDMode;

import edu.wpi.first.apriltag.AprilTagFieldLayout;
import edu.wpi.first.apriltag.AprilTagFields;
import edu.wpi.first.math.geometry.Rotation3d;
import edu.wpi.first.math.geometry.Transform3d;
import edu.wpi.first.math.geometry.Translation3d;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;

import java.util.Optional;

import org.photonvision.EstimatedRobotPose;

public class RobotContainer {
    /* Controllers */
    private final Joystick driver = new Joystick(0);
    private final Joystick driverPS5 = new Joystick(1);

    /* Drive Controls */
    private final int forwardBackwardAxis = XboxController.Axis.kLeftY.value;
    private final int leftRightAxis = XboxController.Axis.kLeftX.value;
    private final int rotationAxis = XboxController.Axis.kRightX.value;
    private final int forwardBackwardAxisPS5 = PS4Controller.Axis.kLeftY.value;
    private final int leftRightAxisPS5 = PS4Controller.Axis.kLeftX.value;
    private final int rotationAxisPS5 = PS4Controller.Axis.kRightX.value;

    /* Driver Buttons */
    private final JoystickButton zeroGyro = new JoystickButton(driver, XboxController.Button.kY.value);
    private final JoystickButton zeroGyroPS5 = new JoystickButton(driverPS5, PS4Controller.Button.kTriangle.value);

    private final JoystickButton robotCentric = new JoystickButton(driver, XboxController.Button.kLeftBumper.value);
    private final JoystickButton robotCentricPS5 = new JoystickButton(driverPS5, PS4Controller.Button.kL1.value);

    private final JoystickButton resetPose = new JoystickButton(driver, XboxController.Button.kX.value);
    private final JoystickButton resetPosePS5 = new JoystickButton(driverPS5, PS4Controller.Button.kSquare.value);

    private final JoystickButton goToCenter = new JoystickButton(driver, XboxController.Button.kStart.value);
    private final JoystickButton goToCenterPS5 = new JoystickButton(driverPS5, PS4Controller.Button.kShare.value);

    private final JoystickButton tryToBalance = new JoystickButton(driver, XboxController.Button.kRightBumper.value);
    // todo figure out what the ps5 equivalent of B is?
    //private final JoystickButton tryToBalancePS5 = new JoystickButton(driverPS5, PS4Controller.Button.kc.value);


    private final JoystickButton intakeIn = new JoystickButton(driver, XboxController.Button.kA.value);
    private final JoystickButton intakeOut = new JoystickButton(driver, XboxController.Button.kB.value);

    /* Subsystems */
    private final Swerve s_Swerve = new Swerve();

    private final Intake s_Intake = new Intake();

    private AprilTagFieldLayout aprilLayout;
    public static PhotonCamera cam;
    // Cam mounted facing forward, half a meter forward of center, half a meter up from center.
    // TODO: update
    public static final Transform3d robotToCam = new Transform3d(new Translation3d(0, 0.381, 0.381), new Rotation3d(0,0,0));
    public static PhotonPoseEstimator photonPoseEstimator;


    /** The container for the robot. Contains subsystems, IO devices, and commands. */
    public RobotContainer() {
        s_Swerve.setDefaultCommand(
            new TeleopSwerve(
                s_Swerve, 
                () -> -driver.getRawAxis(forwardBackwardAxis) - driverPS5.getRawAxis(forwardBackwardAxisPS5), 
                () -> -driver.getRawAxis(leftRightAxis) - driverPS5.getRawAxis(leftRightAxisPS5), 
                () -> -driver.getRawAxis(rotationAxis) - driverPS5.getRawAxis(rotationAxisPS5), 
                () -> robotCentric.getAsBoolean() || robotCentricPS5.getAsBoolean()
            )
        );
        try {aprilLayout = AprilTagFieldLayout.loadFromResource(AprilTagFields.k2023ChargedUp.m_resourceFile);} catch (Exception e) {};
        cam = new PhotonCamera("OV5647");
        cam.setLED(VisionLEDMode.kBlink);
        photonPoseEstimator = new PhotonPoseEstimator(aprilLayout, PoseStrategy.CLOSEST_TO_REFERENCE_POSE, cam, robotToCam);

        configureButtonBindings();
    }

    private void configureButtonBindings() {
        /* Driver Buttons */
        zeroGyro.onTrue(new InstantCommand(() -> s_Swerve.zeroGyro(), s_Swerve));
        zeroGyroPS5.onTrue(new InstantCommand(() -> s_Swerve.zeroGyro(), s_Swerve));

        InstantCommand resetPoseCmd = new InstantCommand(() -> {
          photonPoseEstimator.setReferencePose(s_Swerve.getPose());
          Optional<EstimatedRobotPose> res = photonPoseEstimator.update();
          if (res.isPresent()) {
            EstimatedRobotPose camPose = res.get();
            s_Swerve.resetOdometry(camPose.estimatedPose.toPose2d());
          }
        }, s_Swerve);
        resetPose.onTrue(resetPoseCmd);
        resetPosePS5.onTrue(resetPoseCmd);

        Command goToCenterCmd = new GoToPoint(s_Swerve, s_Swerve.getPose(), new Pose2d(5, 5, new Rotation2d(0)));
        goToCenter.onTrue(goToCenterCmd);
        goToCenterPS5.onTrue(goToCenterCmd);

        tryToBalance.whileTrue(new BalanceThing(s_Swerve));

        intakeIn.whileTrue(new SpInintake(s_Intake, IntakeSpinStatus.Intake));

        intakeOut.whileTrue(new SpInintake(s_Intake, IntakeSpinStatus.Eject));
    }

    public Command getAutonomousCommand() {
        return new exampleAuto(s_Swerve);
    }
}
