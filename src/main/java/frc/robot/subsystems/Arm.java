package frc.robot.subsystems;

import com.ctre.phoenix.motorcontrol.ControlMode;
import com.ctre.phoenix.motorcontrol.NeutralMode;
import com.ctre.phoenix.motorcontrol.can.TalonFX;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.SubsystemBase;
import frc.robot.Constants;
import java.util.function.DoubleSupplier;

public class Arm extends SubsystemBase {
  private final TalonFX armPivot;

  public Arm() {
    // 512/7 ratio, according to build
      /*
       * |
       * |
       * _ _ back = 83222
       * 
       * 
       * __
       * __  3800
       * 
       * 
       * 
       * 
       * 
       * New measurements:
       * good for scoring level 3: 165000
       * vertical at that point: 123000
       * 
       * ~80000: scoring backwards
       * 
       * 
       * 
       * 
       * 
       * 
       * 
       * 
       * 
       * 105 degrees
       * 80000
       * 
       * 39.062 rotations of the falcon
       * (7/512)
       * 0.534 rotations
       * 
       * 
       * 1024/7
       */
    
    armPivot = new TalonFX(Constants.Arm.armPivotID);
    armPivot.configFactoryDefault();
    armPivot.setInverted(true);
    armPivot.setNeutralMode(NeutralMode.Brake);
  }

  private double lastPowerSet = 0;

  public void move(double speed) {
    lastPowerSet = speed;
    armPivot.set(ControlMode.PercentOutput, speed);
  }

  public Command moveCmd(DoubleSupplier speed) {
    return this.runEnd(() -> move(speed.getAsDouble()), () -> move(0));
  }

  @Override
  public void periodic() {
    SmartDashboard.putNumber("Arm Power: ", lastPowerSet);
  }
}