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
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Objects.isNull;

public class ModelInspectorDialog extends JDialog implements ActionListener, TreeSelectionListener
{
    private Model model;
    private final JTree modelTree;
    private final JTextArea modelLabel;
    private final JTextPane stepsText;
    private final JTextPane resultText;
    private final JButton evalButton;

    static {
        String lookAndFeel = UIManager.getSystemLookAndFeelClassName();
        try
        {
            UIManager.setLookAndFeel(lookAndFeel);
        }
        catch ( ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e )
        {
            e.printStackTrace();
        }
    }

    public ModelInspectorDialog( Model model) {
        setTitle( "Model Inspector" );
        setBounds( 300, 90, 900, 600 );
        setDefaultCloseOperation( DISPOSE_ON_CLOSE );
        setResizable( true );

        Container container = getContentPane();
        container.setLayout(new BorderLayout());

        Model root = model.getRoot();
        DefaultMutableTreeNode top = new DefaultMutableTreeNode(root);
        buildChildNodes( root, top, new IdentityHashMap<>() );
        modelTree = new JTree(top);
        modelTree.addTreeSelectionListener(this);
        modelLabel = new JTextArea();
        //modelLabel.setEnabled( false );
        modelLabel.setEditable( false );


        JSplitPane topPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        JSplitPane leftPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        leftPane.setTopComponent( new JScrollPane(modelTree) );
        leftPane.setBottomComponent( new JScrollPane(modelLabel) );

        topPanel.setLeftComponent( leftPane );

        JLabel elLabel = new JLabel("EL expression:");
        stepsText = new JTextPane();
        JLabel resultLabel = new JLabel("Result:");
        resultText = new JTextPane();

        evalButton = new JButton("Eval");
        evalButton.addActionListener( this );
        JPanel toolbarPanel = new JPanel(new BorderLayout());
        toolbarPanel.add( evalButton, BorderLayout.EAST );

        JPanel stepsPanel = new JPanel(new BorderLayout());
        stepsPanel.add( elLabel, BorderLayout.NORTH );
        stepsPanel.add( new JScrollPane(stepsText), BorderLayout.CENTER );
        stepsPanel.add(toolbarPanel, BorderLayout.SOUTH) ;

        JPanel resultsPanel = new JPanel(new BorderLayout());
        resultsPanel.add( resultLabel, BorderLayout.NORTH );
        resultsPanel.add( new JScrollPane(resultText), BorderLayout.CENTER );

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setTopComponent( stepsPanel );
        splitPane.setBottomComponent( resultsPanel );

        topPanel.setRightComponent( splitPane );

        container.add( topPanel );
    }

    private String getDollarFields( Model model) {
        return model
                .entrySet()
                .stream()
                .filter( e -> e.getKey().startsWith( "$" ) && !e.getKey().startsWith( "$shadow" ) )
                .map( e -> format("%s: %s%n",
                        e.getKey(),
                        e.getValue()
                                .toString()
                                .replaceAll( "\\s*[\\r\\n]+\\s*", " " )) )
                .collect( Collectors.joining());
    }

    private void selectModelNode(ModelNode modelNode) {
        if (modelNode.getModel() instanceof Model) {
            this.model = (Model)modelNode.getModel();
            modelLabel.setText( format("%s%n%s", model.getName(), getDollarFields( model )) );
        } else {
            modelLabel.setText( format("%s: %s", modelNode.getKey(), modelNode.getModel()) );
        }
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
            selectModelNode(modelNode);
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

        public boolean isModel() {
            return model instanceof Model;
        }

        @SuppressWarnings( "unchecked" )
        public Map<String, ?> getMap() {
            return (Map<String, ?>)model;
        }
    }

    @SuppressWarnings( "unchecked" )
    private void buildChildNodes( Map<String, ?> model, DefaultMutableTreeNode parent, IdentityHashMap<Object,Object> alreadySeen) {
        if (alreadySeen.containsKey( model )) {
            System.out.printf( "already seen: %s%n", model);
            return;
        } else {
            alreadySeen.put( model, null );
        }
        model.forEach( (key, value) -> {
            DefaultMutableTreeNode node;
            if (key.startsWith( "$" ) && !key.startsWith( "$shadow" ))
            {
                return;
            }
            if (value instanceof Map )
            {
                node = new DefaultMutableTreeNode( new ModelNode( key, value ) );
                buildChildNodes( ( Map< String, ? > ) value, node, alreadySeen );
            } else {
                node = new DefaultMutableTreeNode(new ModelNode(key, value));
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