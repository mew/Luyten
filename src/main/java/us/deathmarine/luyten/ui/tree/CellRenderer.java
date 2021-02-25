package us.deathmarine.luyten.ui.tree;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import us.deathmarine.luyten.util.ThemeUtil;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;

public class CellRenderer extends DefaultTreeCellRenderer {
	private static final long serialVersionUID = -5691181006363313993L;
	
	@Override
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
			int row, boolean hasFocus) {
		super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        byte[] f = ((TreeNodeUserObject) node.getUserObject()).getFile();
        String fn = getFileName(node).toLowerCase();
        if (node.getChildCount() > 0) {
            setIcon(ThemeUtil.PACKAGE_ICON);
		} else if (f != null) {
            ClassReader reader = new ClassReader(f);
            ClassNode cn = new ClassNode();
            reader.accept(cn, ClassReader.SKIP_CODE);
            setIcon(ThemeUtil.getIconFromClassNode(cn));
        } else if (fn.endsWith(".class") || fn.endsWith(".java")) {
			setIcon(ThemeUtil.CLASS_ICON);
		} else if (fn.endsWith(".yml") || fn.endsWith(".yaml")) {
			setIcon(ThemeUtil.YAML_ICON);
		} else if (fn.endsWith(".kt") || fn.endsWith(".kts")) {
            setIcon(ThemeUtil.KOTLIN_FILE_ICON);
        } else if (fn.endsWith(".js") || fn.endsWith(".jsx") || fn.endsWith(".ts") || fn.endsWith(".tsx")) {
			setIcon(ThemeUtil.JAVASCRIPT_ICON);
		} else if (fn.endsWith(".json")) {
            setIcon(ThemeUtil.JSON_ICON);
        } else if (fn.endsWith(".properties")) {
            setIcon(ThemeUtil.PROPERTIES_ICON);
        } else if (fn.endsWith(".mf")) {
            setIcon(ThemeUtil.MANIFEST_ICON);
        } else if (fn.endsWith(".c")) {
            setIcon(ThemeUtil.C_ICON);
        } else if (fn.endsWith(".cpp")) {
            setIcon(ThemeUtil.CPP_ICON);
        } else if (fn.endsWith(".h")) {
            setIcon(ThemeUtil.H_ICON);
        } else if (fn.endsWith("pom.xml")) {
            setIcon(ThemeUtil.MAVEN_ICON);
        } else if (fn.endsWith("gradle")) {
            setIcon(ThemeUtil.GRADLE_ICON);
        }

		return this;
	}

	public String getFileName(DefaultMutableTreeNode node) {
		return ((TreeNodeUserObject) node.getUserObject()).getOriginalName();
	}
}
