/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */


package org.openmole.ui.workflow.implementation;

import org.netbeans.api.visual.widget.Scene;
import org.openide.util.RequestProcessor;

import javax.swing.*;
import java.awt.*;

/**
 * @author David Kaspar
 */
public class SceneSupport {

    public static void show (final Scene scene) {
        if (SwingUtilities.isEventDispatchThread ())
            showEDT (scene);
        else
            SwingUtilities.invokeLater (new Runnable() {
            @Override
                public void run () {
                    showEDT (scene);
                }
            });
    }

    private static void showEDT (Scene scene) {
        JComponent sceneView = scene.getView ();
        if (sceneView == null)
            sceneView = scene.createView ();
        show (sceneView);
    }

    public static void show (final JComponent sceneView) {
        if (SwingUtilities.isEventDispatchThread ())
            showEDT (sceneView);
        else
            SwingUtilities.invokeLater (new Runnable() {
            @Override
                public void run () {
                    showEDT (sceneView);
                }
            });
    }

    private static void showEDT (JComponent sceneView) {
        JScrollPane panel = new JScrollPane (sceneView);
        panel.getHorizontalScrollBar ().setUnitIncrement (32);
        panel.getHorizontalScrollBar ().setBlockIncrement (256);
        panel.getVerticalScrollBar ().setUnitIncrement (32);
        panel.getVerticalScrollBar ().setBlockIncrement (256);
        showCoreEDT (panel);
    }

    public static void showCore (final JComponent view) {
        if (SwingUtilities.isEventDispatchThread ())
            showCoreEDT (view);
        else
            SwingUtilities.invokeLater (new Runnable() {
            @Override
                public void run () {
                    showCoreEDT (view);
                }
            });
    }
    
    private static void showCoreEDT (JComponent view) {
        int width=800,height=600;
        JFrame frame = new JFrame ();//new JDialog (), true);
        frame.add (view, BorderLayout.CENTER);
        frame.setDefaultCloseOperation (JFrame.DISPOSE_ON_CLOSE);
        java.awt.Dimension screenSize = java.awt.Toolkit.getDefaultToolkit().getScreenSize();
        frame.setBounds((screenSize.width-width)/2, (screenSize.height-height)/2, width, height);
        frame.setVisible (true);
    }

    public static int randInt (int max) {
        return (int) (Math.random () * max);
    }

    public static void invokeLater (final Runnable runnable, int delay) {
        RequestProcessor.getDefault ().post (new Runnable() {
            @Override
            public void run () {
                SwingUtilities.invokeLater (runnable);
            }
        }, delay);
    }

    public static void sleep (int delay) {
        try {
            Thread.sleep (delay);
        } catch (InterruptedException e) {
            e.printStackTrace ();
        }
    }

}