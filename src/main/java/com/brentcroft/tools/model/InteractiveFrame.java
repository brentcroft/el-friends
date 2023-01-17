package com.brentcroft.tools.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Map;

import static java.lang.String.format;
import static java.util.Objects.isNull;

public class InteractiveFrame extends JDialog implements ActionListener, TreeSelectionListener
{
    private final Model model;
    private JTree modelTree;
    private final JTextPane stepsText;
    private final JTextPane resultText;
    private final JButton evalButton;

    public InteractiveFrame(Model model) {
        this.model = model;
        setTitle( "Interactive Steps" );
        setBounds( 300, 90, 900, 600 );
        setDefaultCloseOperation( DISPOSE_ON_CLOSE );
        setResizable( true );

        Container container = getContentPane();
        container.setLayout(new BorderLayout());


        DefaultMutableTreeNode top = new DefaultMutableTreeNode(model);
        buildChildNodes( model, top );
        modelTree = new JTree(top);
        modelTree.addTreeSelectionListener(this);

        JSplitPane topPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        topPanel.setTopComponent( modelTree );

        JLabel elLabel = new JLabel("EL:");
        stepsText = new JTextPane();
        JLabel resultLabel = new JLabel("Result:");
        resultText = new JTextPane();

        evalButton = new JButton("Eval");
        evalButton.addActionListener( this );

        JPanel stepsPanel = new JPanel(new BorderLayout());
        stepsPanel.add( elLabel, BorderLayout.NORTH );
        stepsPanel.add( stepsText, BorderLayout.CENTER );
        stepsPanel.add(evalButton, BorderLayout.SOUTH) ;

        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.add( resultLabel, BorderLayout.NORTH );
        resultsPanel.add( resultText, BorderLayout.CENTER );

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent( stepsPanel );
        splitPane.setBottomComponent( resultsPanel );

        topPanel.setBottomComponent( splitPane );

        container.add( topPanel );
    }

    @Override
    public void valueChanged( TreeSelectionEvent e )
    {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                modelTree.getLastSelectedPathComponent();
        if (node == null) {
            return;
        }
        Object nodeInfo = node.getUserObject();
        if (nodeInfo instanceof ModelNode) {
            ModelNode modelNode = (ModelNode)nodeInfo;
            if (modelNode.isMap()) {
                stepsText.setText( (String)modelNode.getMap().get("$run") );
            }
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class ModelNode {
        private final String key;
        private final Object model;
        public String toString() {
            return model instanceof Map
                    ?  format("%s", key)
                   :  format("%s: %s", key, model);
        }
        public boolean isMap() {
            return model instanceof Map;
        }
        public Map<String, ?> getMap() {
            return (Map<String, ?>)model;
        }
    }

    private void buildChildNodes( Map<String, ?> model, DefaultMutableTreeNode parent) {
        model.forEach( (key, value) -> {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(new ModelNode(key, value));
            if (value instanceof Map ) {
                buildChildNodes( ( Map< String, ? > )value, node );
            }
            parent.add( node );
        } );
    }

    public void setSteps(String steps) {
        this.stepsText.setText( steps );
    }

    @Override
    public void actionPerformed( ActionEvent e )
    {
        if (e.getSource() == evalButton) {
            String el = stepsText.getText();
            try {
                Object evalResult = model.eval( el );
                if (isNull(evalResult)) {
                    resultText.setText( "null" );
                } else {
                    resultText.setText( evalResult.toString() );
                }
            } catch (Exception ex) {
                resultText.setText( ex.toString() );
                ex.printStackTrace();
            }
        }
    }
}
