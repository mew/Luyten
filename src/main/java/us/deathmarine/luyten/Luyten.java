package us.deathmarine.luyten;

import com.github.weisj.darklaf.LafManager;
import com.github.weisj.darklaf.theme.event.ThemeInstalledListener;
import us.deathmarine.luyten.config.ConfigSaver;
import us.deathmarine.luyten.ui.window.MainWindow;
import us.deathmarine.luyten.util.ThemeUtil;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.text.DefaultEditorKit;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Starter, the main class
 */
public class Luyten {
    private static final String VERSION = "0.5.4";
	public static final AtomicReference<MainWindow> mainWindowRef = new AtomicReference<>();
	private static final List<File> pendingFiles = new ArrayList<>();

	public static void main(String[] args) {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        System.setProperty("sun.java2d.xrender", "true");

		// for TotalCommander External Viewer setting:
		// javaw -jar "c:\Program Files\Luyten\luyten.jar"
		// (TC will not complain about temporary file when opening .class from
		// .zip or .jar)
		final File fileFromCommandLine = getFileFromCommandLine(args);

		SwingUtilities.invokeLater(() -> {
            LafManager.install(ThemeUtil.THEME_MAP.getOrDefault(ConfigSaver.getLoadedInstance().getLuytenPreferences().getTheme(), LafManager.getInstalledTheme()));
            LafManager.addThemeChangeListener((ThemeInstalledListener) e -> ThemeUtil.onThemeChange());

            if (!mainWindowRef.compareAndSet(null, new MainWindow(fileFromCommandLine))) {
                // Already set - so add the files to open
                openFileInInstance(fileFromCommandLine);
            }
            processPendingFiles();

            mainWindowRef.get().setVisible(true);
        });
	}

	// Private function which processes all pending files - synchronized on the
	// list of pending files
	private static void processPendingFiles() {
		final MainWindow mainWindow = mainWindowRef.get();
		if (mainWindow != null) {
			synchronized (pendingFiles) {
				for (File f : pendingFiles) {
					mainWindow.getModel().loadFile(f);
				}
				pendingFiles.clear();
			}
		}
	}

	// Function which opens the given file in the instance, if it's running -
	// and if not, it processes the files
	public static void openFileInInstance(File fileToOpen) {
		synchronized (pendingFiles) {
			if (fileToOpen != null) {
				pendingFiles.add(fileToOpen);
			}
		}
		processPendingFiles();
	}

	// Function which exits the application if it's running
	public static void quitInstance() {
		final MainWindow mainWindow = mainWindowRef.get();
		if (mainWindow != null) {
			mainWindow.onExitMenu();
		}
	}

	public static File getFileFromCommandLine(String[] args) {
		File fileFromCommandLine = null;
		try {
			if (args.length > 0) {
				String realFileName = new File(args[0]).getCanonicalPath();
				fileFromCommandLine = new File(realFileName);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return fileFromCommandLine;
	}

	public static String getVersion() {
	    return VERSION;
	}

	/**
	 * Method allows for users to copy the stacktrace for reporting any issues.
	 * Add Cool Hyperlink Enhanced for mouse users.
	 * 
	 * @param message
	 * @param e
	 */
	public static void showExceptionDialog(String message, Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String stacktrace = sw.toString();
		try {
			sw.close();
			pw.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		System.out.println(stacktrace);

		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
		if (message.contains("\n")) {
			for (String s : message.split("\n")) {
				pane.add(new JLabel(s));
			}
		} else {
			pane.add(new JLabel(message));
		}
		pane.add(new JLabel(" \n")); // Whitespace
		final JTextArea exception = new JTextArea(25, 100);
		exception.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
		exception.setText(stacktrace);
		exception.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					new JPopupMenu() {
						{
							JMenuItem menuitem = new JMenuItem("Select All");
							menuitem.addActionListener(e12 -> {
                                exception.requestFocus();
                                exception.selectAll();
                            });
							this.add(menuitem);
							menuitem = new JMenuItem("Copy");
							menuitem.addActionListener(new DefaultEditorKit.CopyAction());
							this.add(menuitem);
						}

						private static final long serialVersionUID = 562054483562666832L;
					}.show(e.getComponent(), e.getX(), e.getY());
				}
			}
		});
		JScrollPane scroll = new JScrollPane(exception);
		scroll.setBorder(new CompoundBorder(BorderFactory.createTitledBorder("Stacktrace"),
				new BevelBorder(BevelBorder.LOWERED)));
		pane.add(scroll);
		final String issue = "https://github.com/deathmarine/Luyten/issues";
		final JLabel link = new JLabel("<HTML>Submit to <FONT color=\"#000099\"><U>" + issue + "</U></FONT></HTML>");
		link.setCursor(new Cursor(Cursor.HAND_CURSOR));
		link.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				try {
					Desktop.getDesktop().browse(new URI(issue));
				} catch (Exception e1) {
					e1.printStackTrace();
				}
			}

			@Override
			public void mouseEntered(MouseEvent e) {
				link.setText("<HTML>Submit to <FONT color=\"#00aa99\"><U>" + issue + "</U></FONT></HTML>");
			}

			@Override
			public void mouseExited(MouseEvent e) {
				link.setText("<HTML>Submit to <FONT color=\"#000099\"><U>" + issue + "</U></FONT></HTML>");
			}
		});
		pane.add(link);
		JOptionPane.showMessageDialog(null, pane, "Error!", JOptionPane.ERROR_MESSAGE);
	}
}
