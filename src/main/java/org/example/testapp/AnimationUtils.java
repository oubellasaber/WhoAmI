package org.example.testapp;

import javafx.animation.*;
import javafx.scene.Node;
import javafx.util.Duration;

/**
 * Utility class for creating smooth animations throughout the application.
 */
public class AnimationUtils {

  /**
   * Create a fade-in animation for a node.
   */
  public static FadeTransition fadeIn(Node node, Duration duration) {
    FadeTransition fade = new FadeTransition(duration, node);
    fade.setFromValue(0.0);
    fade.setToValue(1.0);
    return fade;
  }

  /**
   * Create a fade-out animation for a node.
   */
  public static FadeTransition fadeOut(Node node, Duration duration) {
    FadeTransition fade = new FadeTransition(duration, node);
    fade.setFromValue(1.0);
    fade.setToValue(0.0);
    return fade;
  }

  /**
   * Create a scale-up animation for a node (pulse effect).
   */
  public static ScaleTransition scaleUp(Node node, Duration duration) {
    ScaleTransition scale = new ScaleTransition(duration, node);
    scale.setFromX(0.8);
    scale.setFromY(0.8);
    scale.setToX(1.0);
    scale.setToY(1.0);
    return scale;
  }

  /**
   * Create a scale-down animation for a node.
   */
  public static ScaleTransition scaleDown(Node node, Duration duration) {
    ScaleTransition scale = new ScaleTransition(duration, node);
    scale.setFromX(1.0);
    scale.setFromY(1.0);
    scale.setToX(0.95);
    scale.setToY(0.95);
    return scale;
  }

  /**
   * Create a slide-down animation for a node.
   */
  public static TranslateTransition slideDown(Node node, Duration duration, double distance) {
    TranslateTransition slide = new TranslateTransition(duration, node);
    slide.setFromY(-distance);
    slide.setToY(0);
    return slide;
  }

  /**
   * Create a slide-up animation for a node.
   */
  public static TranslateTransition slideUp(Node node, Duration duration, double distance) {
    TranslateTransition slide = new TranslateTransition(duration, node);
    slide.setFromY(0);
    slide.setToY(-distance);
    return slide;
  }

  /**
   * Create a combined fade and scale animation for smooth appearance.
   */
  public static ParallelTransition fadeScaleIn(Node node, Duration duration) {
    FadeTransition fade = fadeIn(node, duration);
    ScaleTransition scale = scaleUp(node, duration);
    return new ParallelTransition(fade, scale);
  }

  /**
   * Create a combined fade and scale animation for smooth disappearance.
   */
  public static ParallelTransition fadeScaleOut(Node node, Duration duration) {
    FadeTransition fade = fadeOut(node, duration);
    ScaleTransition scale = scaleDown(node, duration);
    return new ParallelTransition(fade, scale);
  }

  /**
   * Create a bounce animation (scale pulse effect).
   */
  public static ScaleTransition bounce(Node node) {
    ScaleTransition bounce = new ScaleTransition(Duration.millis(150), node);
    bounce.setFromX(1.0);
    bounce.setFromY(1.0);
    bounce.setToX(1.1);
    bounce.setToY(1.1);
    bounce.setCycleCount(2);
    bounce.setAutoReverse(true);
    return bounce;
  }

  /**
   * Create a highlight animation (fade in/out white overlay effect).
   */
  public static SequentialTransition highlight(Node node) {
    FadeTransition fadeIn = fadeIn(node, Duration.millis(200));
    FadeTransition fadeOut = fadeOut(node, Duration.millis(200));
    return new SequentialTransition(fadeIn, fadeOut);
  }

  /**
   * Create a smooth rotation animation.
   */
  public static RotateTransition rotate(Node node, Duration duration, double angle) {
    RotateTransition rotation = new RotateTransition(duration, node);
    rotation.setFromAngle(0);
    rotation.setToAngle(angle);
    return rotation;
  }

  /**
   * Create a spinning animation (full rotation).
   */
  public static RotateTransition spin(Node node) {
    RotateTransition spin = new RotateTransition(Duration.millis(600), node);
    spin.setFromAngle(0);
    spin.setToAngle(360);
    return spin;
  }
}
