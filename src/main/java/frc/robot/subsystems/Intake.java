package frc.robot.subsystems;

import com.revrobotics.CANSparkMax;
import com.revrobotics.CANSparkMax.IdleMode;
import com.revrobotics.CANSparkMaxLowLevel.MotorType;

import edu.wpi.first.wpilibj.DoubleSolenoid;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;

public class Intake extends SubsystemBase {
  private final CANSparkMax leftIntake;
  private final CANSparkMax rightIntake;

  private final DoubleSolenoid leftSolenoid;
  private final DoubleSolenoid rightSolenoid;

  public double lastPowerSet = 0;

  public enum IntakePistonStatus {
    Cone,
    Cube
  }


  public Intake() {
    leftIntake = new CANSparkMax(Constants.Intake.leftIntakeID, MotorType.kBrushless);
    leftIntake.restoreFactoryDefaults();
    leftIntake.setInverted(false);
    leftIntake.enableVoltageCompensation(11);
    leftIntake.setIdleMode(IdleMode.kBrake);

    rightIntake = new CANSparkMax(Constants.Intake.rightIntakeID, MotorType.kBrushless);
    rightIntake.restoreFactoryDefaults();
    rightIntake.setInverted(false);
    rightIntake.enableVoltageCompensation(11);
    rightIntake.setIdleMode(IdleMode.kBrake);
    DoubleSolenoid leftSolenoid = new DoubleSolenoid(Constants.Intake.pneumaticType, 4, 5);

    DoubleSolenoid rightSolenoid = new DoubleSolenoid(Constants.Intake.pneumaticType, 4, 5);

  }

  public void intakeSpin(double intakeSpeed) {
    lastPowerSet = intakeSpeed;
    leftIntake.set(intakeSpeed);
    rightIntake.set(-intakeSpeed);
  }

  public void

  @Override
  public void periodic() {
    SmartDashboard.putNumber("Intake Status: ", lastPowerSet);
  }
}