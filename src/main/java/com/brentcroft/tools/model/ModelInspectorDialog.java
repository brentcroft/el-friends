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

    public ModelInspectorDialog( Map< String, ? > model) {
        setTitle( "Model Inspector" );
        setBounds( 300, 90, 900, 600 );
        setDefaultCloseOperation( DISPOSE_ON_CLOSE );
        setResizable( true );

        Container container = getContentPane();
        container.setLayout(new BorderLayout());

        Map< String, ? > root = model instanceof Model
                                ? ((Model)model).getRoot()
                                : model;

        this.model = model instanceof Model
                     ? ((Model)model)
                     : null;

        DefaultMutableTreeNode top = new DefaultMutableTreeNode( root );
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
                .filter( e -> e.getKey().startsWith( "$" ) && !e.getKey().startsWith( "$shadow" )  && !e.getKey().startsWith( "$$" ) )
                .sorted( Map.Entry.comparingByKey() )
                .map( e -> format("%s: %s%n",
                        e.getKey(),
                        e.getValue()
                                .toString()
                                .replaceAll( "\\s*[\\r\\n]+\\s*", " " )) )
                .collect( Collectors.joining());
    }

    private void selectModelNode(ModelNode modelNode) {
        this.model = modelNode.getAncestorModel();
        if (modelNode.getModel() instanceof Model) {
            modelLabel.setText( format("%s",getDollarFields( model )) );
            stepsText.setText( modelNode.getKey() );
        } else if (modelNode.getKey().startsWith( "$$" )) {
            modelLabel.setText( format("%s", modelNode.getKey()) );
            stepsText.setText( modelNode.getModel().toString() );
        } else {
            modelLabel.setText( format("%s", modelNode.getModel()) );
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
            selectModelNode(modelNode);
        }
    }

    @Getter
    @Setter
    @AllArgsConstructor
    public static class ModelNode {
        private final Object parent;
        private final String key;
        private final Object model;

        public String toString() {
            return format("%s", key);
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

        public Model getAncestorModel()
        {
            if (model instanceof Model) {
                return (Model)model;
            } else if (parent instanceof ModelNode) {
                return ((ModelNode)parent).getAncestorModel();
            } else {
                return null;
            }
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
        model
                .entrySet()
                .stream()
                .sorted( Map.Entry.comparingByKey() )
                .forEachOrdered( e -> {
                    String key = e.getKey();
                    Object value = e.getValue();
                    DefaultMutableTreeNode node;
                    if (key.startsWith( "$" ) && !key.startsWith( "$shadow" ) && !key.startsWith( "$$" ))
                    {
                        return;
                    }
                    node = new DefaultMutableTreeNode( new ModelNode(parent.getUserObject(), key, value ) );
                    if (value instanceof Map )
                    {
                        buildChildNodes( ( Map< String, ? > ) value, node, alreadySeen );
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
                Object evalResult = model.eval( model.expand( el ) );
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
