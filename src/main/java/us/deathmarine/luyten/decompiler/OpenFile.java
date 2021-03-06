package us.deathmarine.luyten.decompiler;

import com.strobel.assembler.metadata.MetadataSystem;
import com.strobel.assembler.metadata.TypeDefinition;
import com.strobel.decompiler.DecompilationOptions;
import com.strobel.decompiler.DecompilerSettings;
import com.strobel.decompiler.PlainTextOutput;
import com.strobel.decompiler.languages.Languages;
import org.benf.cfr.reader.api.CfrDriver;
import org.benf.cfr.reader.api.ClassFileSource;
import org.benf.cfr.reader.api.OutputSinkFactory;
import org.benf.cfr.reader.api.SinkReturns;
import org.benf.cfr.reader.bytecode.analysis.parse.utils.Pair;
import org.fife.ui.rsyntaxtextarea.LinkGeneratorResult;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;
import org.fife.ui.rsyntaxtextarea.Theme;
import org.fife.ui.rtextarea.RTextScrollPane;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import us.deathmarine.luyten.Luyten;
import us.deathmarine.luyten.config.ConfigSaver;
import us.deathmarine.luyten.config.LuytenPreferences;
import us.deathmarine.luyten.ui.JFontChooser;
import us.deathmarine.luyten.ui.window.MainWindow;
import us.deathmarine.luyten.util.Selection;
import us.deathmarine.luyten.util.system.Keymap;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class OpenFile implements SyntaxConstants {

	public static final HashSet<String> WELL_KNOWN_TEXT_FILE_EXTENSIONS = new HashSet<>(
			Arrays.asList(".java", ".xml", ".rss", ".project", ".classpath", ".h", ".c", ".cpp", ".yaml", ".yml", ".ini", ".sql", ".js", ".php", ".php5",
					".phtml", ".html", ".htm", ".xhtm", ".xhtml", ".lua", ".bat", ".pl", ".sh", ".css", ".json", ".txt",
					".rb", ".make", ".mak", ".py", ".properties", ".prop", ".scala", ".svg"));

	// navigation links
	private TreeMap<Selection, String> selectionToUniqueStrTreeMap = new TreeMap<>();
	private final Map<String, Boolean> isNavigableCache = new ConcurrentHashMap<>();
	private final Map<String, String> readableLinksCache = new ConcurrentHashMap<>();

	private volatile boolean isContentValid = false;
	private volatile boolean isNavigationLinksValid = false;
	private volatile boolean isWaitForLinksCursor = false;
	private volatile Double lastScrollPercent = null;

	private LinkProvider linkProvider;
	private String initialNavigationLink;
	private boolean isFirstTimeRun = true;

	public MainWindow mainWindow;
	public RTextScrollPane scrollPane;
	public Panel image_pane;
	public RSyntaxTextArea textArea;
	public String name;
	public String path;

	private byte[] classBytes;
	private ConfigSaver configSaver;
	private LuytenPreferences luytenPrefs;

	// decompiler and type references (not needed for text files)
	private MetadataSystem metadataSystem;
	private DecompilerSettings settings;
	private DecompilationOptions decompilationOptions;
	private TypeDefinition type;

	public OpenFile(String name, String path, Theme theme, byte[] classBytes, final MainWindow mainWindow) {
		this.name = name;
		this.path = path;
		this.mainWindow = mainWindow;
		this.classBytes = classBytes;

		configSaver = ConfigSaver.getLoadedInstance();
		luytenPrefs = configSaver.getLuytenPreferences();
		
		textArea = new RSyntaxTextArea(25, 70);
		textArea.setCaretPosition(0);
		textArea.requestFocusInWindow();
		textArea.setMarkOccurrences(true);
		textArea.setClearWhitespaceLinesEnabled(false);
		textArea.setEditable(false);
		textArea.setAntiAliasingEnabled(true);
		textArea.setCodeFoldingEnabled(true);

		String[] splitFileName = name.toLowerCase().split("\\.");
		String last = splitFileName[splitFileName.length - 1];

		String syntaxHighlighting;
		switch (last) {
            case "class":
            case "java":
                syntaxHighlighting = SYNTAX_STYLE_JAVA;
                break;
            case "xml":
            case "rss":
            case "project":
            case "classpath":
            case "svg":
                syntaxHighlighting = SYNTAX_STYLE_XML;
                break;
            case "h":
            case "c":
                syntaxHighlighting = SYNTAX_STYLE_C;
                break;
            case "cpp":
                syntaxHighlighting = SYNTAX_STYLE_CPLUSPLUS;
                break;
            case "cs":
                syntaxHighlighting = SYNTAX_STYLE_CSHARP;
                break;
            case "groovy":
            case "gradle":
                syntaxHighlighting = SYNTAX_STYLE_GROOVY;
                break;
            case "sql":
                syntaxHighlighting = SYNTAX_STYLE_SQL;
                break;
            case "js":
                syntaxHighlighting = SYNTAX_STYLE_JAVASCRIPT;
                break;
            case "ts":
                syntaxHighlighting = SYNTAX_STYLE_TYPESCRIPT;
                break;
            case "php":
            case "php5":
            case "phtml":
                syntaxHighlighting = SYNTAX_STYLE_PHP;
                break;
            case "html":
            case "htm":
            case "xhtm":
            case "xhtml":
                syntaxHighlighting = SYNTAX_STYLE_HTML;
                break;
            case "lua":
                syntaxHighlighting = SYNTAX_STYLE_LUA;
                break;
            case "bat":
                syntaxHighlighting = SYNTAX_STYLE_WINDOWS_BATCH;
                break;
            case "sh":
                syntaxHighlighting = SYNTAX_STYLE_UNIX_SHELL;
                break;
            case "ini":
                syntaxHighlighting = SYNTAX_STYLE_INI;
                break;
            case "yaml":
            case "yml":
                syntaxHighlighting = SYNTAX_STYLE_YAML;
                break;
            case "py":
                syntaxHighlighting = SYNTAX_STYLE_PYTHON;
                break;
            case "scala":
                syntaxHighlighting = SYNTAX_STYLE_SCALA;
                break;
            default:
                syntaxHighlighting = SYNTAX_STYLE_NONE;
        }
        textArea.setSyntaxEditingStyle(syntaxHighlighting);
		scrollPane = new RTextScrollPane(textArea, true);

		scrollPane.setIconRowHeaderEnabled(true);
		textArea.setText("");

		// Edit RTextArea's PopupMenu
		JPopupMenu pop = textArea.getPopupMenu();
		pop.addSeparator();
		JMenuItem item = new JMenuItem("Font");
		item.addActionListener(e -> {
            JFontChooser fontChooser = new JFontChooser();
            fontChooser.setSelectedFont(textArea.getFont());
            fontChooser.setSelectedFontSize(textArea.getFont().getSize());
            int result = fontChooser.showDialog(mainWindow);
            if (result == JFontChooser.OK_OPTION) {
                textArea.setFont(fontChooser.getSelectedFont());
                luytenPrefs.setFont_size(fontChooser.getSelectedFontSize());
            }
        });
		pop.add(item);
		textArea.setPopupMenu(pop);
		
		theme.apply(textArea);

		textArea.setFont(new Font(textArea.getFont().getName(), textArea.getFont().getStyle(), luytenPrefs.getFont_size()));
		
		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		final JScrollBar verticalScrollbar = scrollPane.getVerticalScrollBar();
		if (verticalScrollbar != null) {
			verticalScrollbar.addAdjustmentListener(e -> {
                String content = textArea.getText();
                if (content == null || content.length() == 0)
                    return;
                int scrollValue = verticalScrollbar.getValue() - verticalScrollbar.getMinimum();
                int scrollMax = verticalScrollbar.getMaximum() - verticalScrollbar.getMinimum();
                if (scrollMax < 1 || scrollValue < 0 || scrollValue > scrollMax)
                    return;
                lastScrollPercent = (((double) scrollValue) / ((double) scrollMax));
            });
		}

		textArea.setHyperlinksEnabled(true);
		textArea.setLinkScanningMask(Keymap.ctrlDownModifier());

		textArea.setLinkGenerator((textArea, offs) -> {
            final String uniqueStr = getUniqueStrForOffset(offs);
            final Integer selectionFrom = getSelectionFromForOffset(offs);
            if (uniqueStr != null && selectionFrom != null) {
                return new LinkGeneratorResult() {
                    @Override
                    public HyperlinkEvent execute() {
                        if (isNavigationLinksValid)
                            onNavigationClicked(uniqueStr);
                        return null;
                    }

                    @Override
                    public int getSourceOffset() {
                        if (isNavigationLinksValid)
                            return selectionFrom;
                        return offs;
                    }
                };
            }
            return null;
        });

		/*
		 * Add Ctrl+Wheel Zoom for Text Size Removes all standard listeners and
		 * writes new listeners for wheelscroll movement.
		 */
		for (MouseWheelListener listeners : scrollPane.getMouseWheelListeners()) {
			scrollPane.removeMouseWheelListener(listeners);
		}

		scrollPane.addMouseWheelListener(e -> {
            if (e.getWheelRotation() == 0) {
                // Nothing to do here. This happens when scroll event is delivered from a touchbar
                // or MagicMouse. There's getPreciseWheelRotation, however it looks like there's no
                // trivial and consistent way to use that
                // See https://github.com/JetBrains/intellij-community/blob/21c99af7c78fc82aefc4d05646389f4991b08b38/bin/idea.properties#L133-L156
                return;
            }

            if ((e.getModifiersEx() & Keymap.ctrlDownModifier()) != 0) {
                Font font = textArea.getFont();
                int size = font.getSize();
                if (e.getWheelRotation() > 0) {
                    textArea.setFont(new Font(font.getName(), font.getStyle(), --size >= 8 ? --size : 8));
                } else {
                    textArea.setFont(new Font(font.getName(), font.getStyle(), ++size));
                }
                luytenPrefs.setFont_size(size);
            } else {
                if (scrollPane.isWheelScrollingEnabled() && e.getWheelRotation() != 0) {
                    JScrollBar toScroll = scrollPane.getVerticalScrollBar();
                    int direction = e.getWheelRotation() < 0 ? -1 : 1;
                    int orientation = SwingConstants.VERTICAL;
                    if (toScroll == null || !toScroll.isVisible()) {
                        toScroll = scrollPane.getHorizontalScrollBar();
                        if (toScroll == null || !toScroll.isVisible()) {
                            return;
                        }
                        orientation = SwingConstants.HORIZONTAL;
                    }
                    e.consume();

                    if (e.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) {
                        JViewport vp = scrollPane.getViewport();
                        if (vp == null) {
                            return;
                        }
                        Component comp = vp.getView();
                        int units = Math.abs(e.getUnitsToScroll());
                        boolean limitScroll = Math.abs(e.getWheelRotation()) == 1;
                        Object fastWheelScroll = toScroll.getClientProperty("JScrollBar.fastWheelScrolling");
                        if (Boolean.TRUE == fastWheelScroll && comp instanceof Scrollable) {
                            Scrollable scrollComp = (Scrollable) comp;
                            Rectangle viewRect = vp.getViewRect();
                            int startingX = viewRect.x;
                            boolean leftToRight = comp.getComponentOrientation().isLeftToRight();
                            int scrollMin = toScroll.getMinimum();
                            int scrollMax = toScroll.getMaximum() - toScroll.getModel().getExtent();

                            if (limitScroll) {
                                int blockIncr = scrollComp.getScrollableBlockIncrement(viewRect, orientation,
                                        direction);
                                if (direction < 0) {
                                    scrollMin = Math.max(scrollMin, toScroll.getValue() - blockIncr);
                                } else {
                                    scrollMax = Math.min(scrollMax, toScroll.getValue() + blockIncr);
                                }
                            }

                            for (int i = 0; i < units; i++) {
                                int unitIncr = scrollComp.getScrollableUnitIncrement(viewRect, orientation,
                                        direction);
                                if (orientation == SwingConstants.VERTICAL) {
                                    if (direction < 0) {
                                        viewRect.y -= unitIncr;
                                        if (viewRect.y <= scrollMin) {
                                            viewRect.y = scrollMin;
                                            break;
                                        }
                                    } else { // (direction > 0
                                        viewRect.y += unitIncr;
                                        if (viewRect.y >= scrollMax) {
                                            viewRect.y = scrollMax;
                                            break;
                                        }
                                    }
                                } else {
                                    if ((leftToRight && direction < 0) || (!leftToRight && direction > 0)) {
                                        viewRect.x -= unitIncr;
                                        if (leftToRight) {
                                            if (viewRect.x < scrollMin) {
                                                viewRect.x = scrollMin;
                                                break;
                                            }
                                        }
                                    } else if ((leftToRight && direction > 0) || (!leftToRight && direction < 0)) {
                                        viewRect.x += unitIncr;
                                        if (leftToRight) {
                                            if (viewRect.x > scrollMax) {
                                                viewRect.x = scrollMax;
                                                break;
                                            }
                                        }
                                    } else {
                                        assert false : "Non-sensical ComponentOrientation / scroll direction";
                                    }
                                }
                            }
                            if (orientation == SwingConstants.VERTICAL) {
                                toScroll.setValue(viewRect.y);
                            } else {
                                if (leftToRight) {
                                    toScroll.setValue(viewRect.x);
                                } else {
                                    int newPos = toScroll.getValue() - (viewRect.x - startingX);
                                    if (newPos < scrollMin) {
                                        newPos = scrollMin;
                                    } else if (newPos > scrollMax) {
                                        newPos = scrollMax;
                                    }
                                    toScroll.setValue(newPos);
                                }
                            }
                        } else {
                            int delta;
                            int limit = -1;

                            if (limitScroll) {
                                if (direction < 0) {
                                    limit = toScroll.getValue() - toScroll.getBlockIncrement(direction);
                                } else {
                                    limit = toScroll.getValue() + toScroll.getBlockIncrement(direction);
                                }
                            }

                            for (int i = 0; i < units; i++) {
                                if (direction > 0) {
                                    delta = toScroll.getUnitIncrement(direction);
                                } else {
                                    delta = -toScroll.getUnitIncrement(direction);
                                }
                                int oldValue = toScroll.getValue();
                                int newValue = oldValue + delta;
                                if (delta > 0 && newValue < oldValue) {
                                    newValue = toScroll.getMaximum();
                                } else if (delta < 0 && newValue > oldValue) {
                                    newValue = toScroll.getMinimum();
                                }
                                if (oldValue == newValue) {
                                    break;
                                }
                                if (limitScroll && i > 0) {
                                    assert limit != -1;
                                    if ((direction < 0 && newValue < limit)
                                            || (direction > 0 && newValue > limit)) {
                                        break;
                                    }
                                }
                                toScroll.setValue(newValue);
                            }

                        }
                    } else if (e.getScrollType() == MouseWheelEvent.WHEEL_BLOCK_SCROLL) {
                        int oldValue = toScroll.getValue();
                        int blockIncrement = toScroll.getBlockIncrement(direction);
                        int delta = blockIncrement * ((direction > 0) ? +1 : -1);
                        int newValue = oldValue + delta;
                        if (delta > 0 && newValue < oldValue) {
                            newValue = toScroll.getMaximum();
                        } else if (delta < 0 && newValue > oldValue) {
                            newValue = toScroll.getMinimum();
                        }
                        toScroll.setValue(newValue);
                    }
                }
            }

            e.consume();
        });

		textArea.addMouseMotionListener(new MouseMotionAdapter() {
			private boolean isLinkLabelPrev = false;
			private String prevLinkText = null;

			@Override
			public synchronized void mouseMoved(MouseEvent e) {
				String linkText = null;
				boolean isLinkLabel = false;
				boolean isCtrlDown = (e.getModifiersEx() & Keymap.ctrlDownModifier()) != 0;
				if (isCtrlDown) {
					linkText = createLinkLabel(e);
					isLinkLabel = linkText != null;
				}
				if (isCtrlDown && isWaitForLinksCursor) {
					textArea.setCursor(new Cursor(Cursor.WAIT_CURSOR));
				} else if (textArea.getCursor().getType() == Cursor.WAIT_CURSOR) {
					textArea.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}

				JLabel label = OpenFile.this.mainWindow.getLabel();

				if (isLinkLabel && isLinkLabelPrev) {
					if (!linkText.equals(prevLinkText)) {
						setLinkLabel(label, linkText);
					}
				} else if (isLinkLabel && !isLinkLabelPrev) {
					setLinkLabel(label, linkText);

				} else if (!isLinkLabel && isLinkLabelPrev) {
					setLinkLabel(label, null);
				}
				isLinkLabelPrev = isLinkLabel;
				prevLinkText = linkText;
			}

			private void setLinkLabel(JLabel label, String text) {
				String current = label.getText();
				if (text == null && current != null)
					if (current.startsWith("Navigating:") || current.startsWith("Cannot navigate:"))
						return;
				label.setText(text != null ? text : "Complete");
			}

			private String createLinkLabel(MouseEvent e) {
				int offs = textArea.viewToModel(e.getPoint());
				if (isNavigationLinksValid) {
					return getLinkDescriptionForOffset(offs);
				}
				return null;
			}
		});
	}

	public void setContent(String content) {
		textArea.setText(content);
	}

	public void decompile() {
		this.invalidateContent();
		// synchronized: do not accept changes from menu while running
		synchronized (settings) {
			if (Languages.java().getName().equals(settings.getLanguage().getName())) {
				decompileWithNavigationLinks();
			} else {
				decompileWithoutLinks();
			}
		}
	}

	private void decompileWithoutLinks() {
		this.invalidateContent();
		isNavigationLinksValid = false;
		textArea.setHyperlinksEnabled(false);

		StringWriter stringwriter = new StringWriter();
		PlainTextOutput plainTextOutput = new PlainTextOutput(stringwriter);
		plainTextOutput.setUnicodeOutputEnabled(decompilationOptions.getSettings().isUnicodeOutputEnabled());
		settings.getLanguage().decompileType(type, plainTextOutput, decompilationOptions);
		setContentPreserveLastScrollPosition(stringwriter.toString());
		this.isContentValid = true;
	}

	private String cfrOutput = null;

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();
    public static String getFirstBytes(byte[] bytes) {
        boolean bg8 = bytes.length > 8;
        char[] hexChars = new char[bg8 ? 8 : bytes.length * 2];
        for (int i = 0; i < (bg8 ? 4 : bytes.length); i++) {
            int v = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_ARRAY[v >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }
    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return new String(hexChars);
    }

	private void decompileWithNavigationLinks() {
		this.invalidateContent();
		if (classBytes == null || luytenPrefs.getDecompiler().equals("procyon")) {
            DecompilerLinkProvider newLinkProvider = new DecompilerLinkProvider();
            newLinkProvider.setDecompilerReferences(metadataSystem, settings, decompilationOptions);
            newLinkProvider.setType(type);
            linkProvider = newLinkProvider;

            linkProvider.generateContent();
            setContentPreserveLastScrollPosition(linkProvider.getTextContent());
            enableLinks();
        } else {
		    System.out.println(name + ":");
		    System.out.println(getFirstBytes(classBytes));
		    System.out.println("---------------------------------");
            ClassReader cr = new ClassReader(classBytes);
            ClassNode cn = new ClassNode();
            cr.accept(cn, ClassReader.SKIP_CODE);
            System.out.println(cn.name);
            System.out.println(cn.access);
		    try {
                ClassFileSource cfs = new ClassFileSource() {
                    @Override
                    public void informAnalysisRelativePathDetail(String usePath, String classFilePath) {
                    }

                    @Override
                    public Collection<String> addJar(String jarPath) {
                        return null;
                    }

                    @Override
                    public String getPossiblyRenamedPath(String path) {
                        return path;
                    }

                    @Override
                    public Pair<byte[], String> getClassFileContent(String path) throws IOException {
                        // name - .class
                        String fullName = path.substring(0, path.length() - 6);

                        if (fullName.replace('/', '.').equals(type.getFullName())) {
                            return Pair.make(classBytes, fullName);
                        }

                        // uh oh!
                        return null;
                    }
                };
                cfrOutput = null;
                OutputSinkFactory cfrOutputSink = new OutputSinkFactory() {
                    @Override
                    public List<SinkClass> getSupportedSinks(SinkType sinkType, Collection<SinkClass> collection) {
                        if (sinkType == SinkType.JAVA && collection.contains(SinkClass.DECOMPILED)) {
                            return Arrays.asList(SinkClass.DECOMPILED, SinkClass.STRING);
                        } else {
                            return Arrays.asList(SinkClass.STRING, SinkClass.EXCEPTION_MESSAGE);
                        }
                    }

                    @Override
                    public <T> Sink<T> getSink(SinkType sinkType, SinkClass sinkClass) {
                        if (sinkType == SinkType.JAVA && sinkClass == OutputSinkFactory.SinkClass.DECOMPILED) {
                            return x -> cfrOutput = ((SinkReturns.Decompiled) x).getJava();
                        } else if (sinkType == SinkType.EXCEPTION) {
                            return ex -> Luyten.showExceptionDialog("CFR Exception", ((SinkReturns.ExceptionMessage) ex).getThrownException());
                        }
                        return ignore -> { };
                    }
                };
                CfrDriver CFR = new CfrDriver.Builder().withClassFileSource(cfs).withOutputSink(cfrOutputSink).build();
                CFR.analyse(Collections.singletonList(type.getFullName()));
                setContentPreserveLastScrollPosition(cfrOutput != null ? cfrOutput.split("\\*/\n", 2)[1] : "Null CFR output... Maybe it's loading?");
            } catch (Exception e) {
		        StringWriter sw = new StringWriter();
		        e.printStackTrace(new PrintWriter(sw));
		        setContent(sw.toString());
            }
        }
        this.isContentValid = true;
	}

	private void setContentPreserveLastScrollPosition(final String content) {
		final Double scrollPercent = lastScrollPercent;
		if (scrollPercent != null && initialNavigationLink == null) {
			SwingUtilities.invokeLater(() -> {
                textArea.setText(content);
                restoreScrollPosition(scrollPercent);
            });
		} else {
			textArea.setText(content);
		}
	}

	private void restoreScrollPosition(final double position) {
		SwingUtilities.invokeLater(() -> {
            JScrollBar verticalScrollbar = scrollPane.getVerticalScrollBar();
            if (verticalScrollbar == null)
                return;
            int scrollMax = verticalScrollbar.getMaximum() - verticalScrollbar.getMinimum();
            long newScrollValue = Math.round(position * scrollMax) + verticalScrollbar.getMinimum();
            if (newScrollValue < verticalScrollbar.getMinimum())
                newScrollValue = verticalScrollbar.getMinimum();
            if (newScrollValue > verticalScrollbar.getMaximum())
                newScrollValue = verticalScrollbar.getMaximum();
            verticalScrollbar.setValue((int) newScrollValue);
        });
	}

	private void enableLinks() {
		if (initialNavigationLink != null) {
			doEnableLinks();
		} else {
			new Thread(() -> {
                try {
                    isWaitForLinksCursor = true;
                    doEnableLinks();
                } finally {
                    isWaitForLinksCursor = false;
                    resetCursor();
                }
            }).start();
		}
	}

	private void resetCursor() {
		SwingUtilities.invokeLater(() -> textArea.setCursor(new Cursor(Cursor.DEFAULT_CURSOR)));
	}

	private void doEnableLinks() {
		isNavigationLinksValid = false;
		linkProvider.processLinks();
		buildSelectionToUniqueStrTreeMap();
		clearLinksCache();
		isNavigationLinksValid = true;
		textArea.setHyperlinksEnabled(true);
		warmUpWithFirstLink();
	}

	private void warmUpWithFirstLink() {
		if (selectionToUniqueStrTreeMap.keySet().size() > 0) {
			Selection selection = selectionToUniqueStrTreeMap.keySet().iterator().next();
			getLinkDescriptionForOffset(selection.from);
		}
	}

	public void clearLinksCache() {
		try {
			isNavigableCache.clear();
			readableLinksCache.clear();
		} catch (Exception e) {
			Luyten.showExceptionDialog("Exception!", e);
		}
	}

	private void buildSelectionToUniqueStrTreeMap() {
		TreeMap<Selection, String> treeMap = new TreeMap<>();
		Map<String, Selection> definitionToSelectionMap = linkProvider.getDefinitionToSelectionMap();
		Map<String, Set<Selection>> referenceToSelectionsMap = linkProvider.getReferenceToSelectionsMap();

		for (String key : definitionToSelectionMap.keySet()) {
			Selection selection = definitionToSelectionMap.get(key);
			treeMap.put(selection, key);
		}
		for (String key : referenceToSelectionsMap.keySet()) {
			for (Selection selection : referenceToSelectionsMap.get(key)) {
				treeMap.put(selection, key);
			}
		}
		selectionToUniqueStrTreeMap = treeMap;
	}

	private Selection getSelectionForOffset(int offset) {
		if (isNavigationLinksValid) {
			Selection offsetSelection = new Selection(offset, offset);
			Selection floorSelection = selectionToUniqueStrTreeMap.floorKey(offsetSelection);
			if (floorSelection != null && floorSelection.from <= offset && floorSelection.to > offset) {
				return floorSelection;
			}
		}
		return null;
	}

	private String getUniqueStrForOffset(int offset) {
		Selection selection = getSelectionForOffset(offset);
		if (selection != null) {
			String uniqueStr = selectionToUniqueStrTreeMap.get(selection);
			if (this.isLinkNavigable(uniqueStr) && this.getLinkDescription(uniqueStr) != null) {
				return uniqueStr;
			}
		}
		return null;
	}

	private Integer getSelectionFromForOffset(int offset) {
		Selection selection = getSelectionForOffset(offset);
		if (selection != null) {
			return selection.from;
		}
		return null;
	}

	private String getLinkDescriptionForOffset(int offset) {
		String uniqueStr = getUniqueStrForOffset(offset);
		if (uniqueStr != null) {
            return this.getLinkDescription(uniqueStr);
		}
		return null;
	}

	private boolean isLinkNavigable(String uniqueStr) {
		try {
			Boolean isNavigableCached = isNavigableCache.get(uniqueStr);
			if (isNavigableCached != null)
				return isNavigableCached;

			boolean isNavigable = linkProvider.isLinkNavigable(uniqueStr);
			isNavigableCache.put(uniqueStr, isNavigable);
			return isNavigable;
		} catch (Exception e) {
			Luyten.showExceptionDialog("Exception!", e);
		}
		return false;
	}

	private String getLinkDescription(String uniqueStr) {
		try {
			String descriptionCached = readableLinksCache.get(uniqueStr);
			if (descriptionCached != null)
				return descriptionCached;

			String description = linkProvider.getLinkDescription(uniqueStr);
			if (description != null && description.trim().length() > 0) {
				readableLinksCache.put(uniqueStr, description);
				return description;
			}
		} catch (Exception e) {
			Luyten.showExceptionDialog("Exception!", e);
		}
		return null;
	}

	private void onNavigationClicked(String clickedReferenceUniqueStr) {
		if (isLocallyNavigable(clickedReferenceUniqueStr)) {
			onLocalNavigationRequest(clickedReferenceUniqueStr);
		} else if (linkProvider.isLinkNavigable(clickedReferenceUniqueStr)) {
			onOutboundNavigationRequest(clickedReferenceUniqueStr);
		} else {
			JLabel label = this.mainWindow.getLabel();
			if (label == null)
				return;
			String[] linkParts = clickedReferenceUniqueStr.split("\\|");
			if (linkParts.length <= 1) {
				label.setText("Cannot navigate: " + clickedReferenceUniqueStr);
				return;
			}
			String destinationTypeStr = linkParts[1];
			label.setText("Cannot navigate: " + destinationTypeStr.replaceAll("/", "."));
		}
	}

	private boolean isLocallyNavigable(String uniqueStr) {
		return linkProvider.getDefinitionToSelectionMap().containsKey(uniqueStr);
	}

	private void onLocalNavigationRequest(String uniqueStr) {
		try {
			Selection selection = linkProvider.getDefinitionToSelectionMap().get(uniqueStr);
			doLocalNavigation(selection);
		} catch (Exception e) {
			Luyten.showExceptionDialog("Exception!", e);
		}
	}

	private void doLocalNavigation(Selection selection) {
		try {
			textArea.requestFocusInWindow();
			if (selection != null) {
				textArea.setSelectionStart(selection.from);
				textArea.setSelectionEnd(selection.to);
				scrollToSelection(selection.from);
			} else {
				textArea.setSelectionStart(0);
				textArea.setSelectionEnd(0);
			}
		} catch (Exception e) {
			Luyten.showExceptionDialog("Exception!", e);
		}
	}

	private void scrollToSelection(final int selectionBeginningOffset) {
		SwingUtilities.invokeLater(() -> {
            try {
                int fullHeight = textArea.getBounds().height;
                int viewportHeight = textArea.getVisibleRect().height;
                int viewportLineCount = viewportHeight / textArea.getLineHeight();
                int selectionLineNum = textArea.getLineOfOffset(selectionBeginningOffset);
                int upperMarginToScroll = Math.round(viewportLineCount * 0.29f);
                int upperLineToSet = selectionLineNum - upperMarginToScroll;
                int currentUpperLine = textArea.getVisibleRect().y / textArea.getLineHeight();

                if (selectionLineNum <= currentUpperLine + 2
                        || selectionLineNum >= currentUpperLine + viewportLineCount - 4) {
                    Rectangle rectToScroll = new Rectangle();
                    rectToScroll.x = 0;
                    rectToScroll.width = 1;
                    rectToScroll.y = Math.max(upperLineToSet * textArea.getLineHeight(), 0);
                    rectToScroll.height = Math.min(viewportHeight, fullHeight - rectToScroll.y);
                    textArea.scrollRectToVisible(rectToScroll);
                }
            } catch (Exception e) {
                Luyten.showExceptionDialog("Exception!", e);
            }
        });
	}

	private void onOutboundNavigationRequest(String uniqueStr) {
		mainWindow.onNavigationRequest(uniqueStr);
	}

	public void setDecompilerReferences(MetadataSystem metadataSystem, DecompilerSettings settings,
			DecompilationOptions decompilationOptions) {
		this.metadataSystem = metadataSystem;
		this.settings = settings;
		this.decompilationOptions = decompilationOptions;
	}

	public TypeDefinition getType() {
		return type;
	}

	public void setType(TypeDefinition type) {
		this.type = type;
	}

	public boolean isContentValid() {
		return isContentValid;
	}

	public void invalidateContent() {
		try {
			this.setContent("");
		} finally {
			this.isContentValid = false;
			this.isNavigationLinksValid = false;
		}
	}

	public void resetScrollPosition() {
		lastScrollPercent = null;
	}

	public void setInitialNavigationLink(String initialNavigationLink) {
		this.initialNavigationLink = initialNavigationLink;
	}

	public void onAddedToScreen() {
		try {
			if (initialNavigationLink != null) {
				onLocalNavigationRequest(initialNavigationLink);
			} else if (isFirstTimeRun) {
				// warm up scrolling
				isFirstTimeRun = false;
				doLocalNavigation(new Selection(0, 0));
			}
		} finally {
			initialNavigationLink = null;
		}
	}

	/**
	 * sun.swing.CachedPainter holds on OpenFile for a while even after
	 * JTabbedPane.remove(component)
	 */
	public void close() {
		linkProvider = null;
		type = null;
		invalidateContent();
		clearLinksCache();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		OpenFile other = (OpenFile) obj;
		if (path == null) {
            return other.path == null;
		} else return path.equals(other.path);
    }

    public byte[] getClassBytes() {
        return classBytes;
    }

    public void setClassBytes(byte[] classBytes) {
        this.classBytes = classBytes;
    }
}
