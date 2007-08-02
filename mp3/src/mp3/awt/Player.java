package mp3.awt;

import java.awt.Button;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.List;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemColor;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class Player implements ActionListener, MouseListener {

    private Font font;
    private Image icon;
    private String TITLE = "MP3 Player";
    private File[] files;
    private List list;
    private PlayerThread thread;

    /**
     * The command line interface for this tool.
     * The command line options are the same as in the Server tool.
     * 
     * @param args the command line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        new Player().run(args);
        // TODO don't open multiple windows
        // TODO show current playing song (file name and time played)
    }

    private void run(String[] args) {
        if(!GraphicsEnvironment.isHeadless()) {
            font = new Font("Dialog", Font.PLAIN, 11);
            try {
                InputStream in = getClass().getResourceAsStream("mp3.png");
                if(in != null) {
                    byte[] imageData = readBytesAndClose(in, -1);
                    icon = Toolkit.getDefaultToolkit().createImage(imageData);
                }
                boolean icon = createTrayIcon();
                open(!icon);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean createTrayIcon() {
        try {
            // SystemTray.isSupported();
            Boolean supported = (Boolean) Class.forName("java.awt.SystemTray").
                getMethod("isSupported", new Class[0]).
                invoke(null, new Object[0]);
            
            if(!supported.booleanValue()) {
                return false;
            }
            
            PopupMenu menuConsole = new PopupMenu();
            MenuItem itemConsole = new MenuItem(TITLE);
            itemConsole.setActionCommand("open");
            itemConsole.addActionListener(this);
            itemConsole.setFont(font);
            menuConsole.add(itemConsole);
            
            MenuItem itemNext = new MenuItem("Next");
            itemNext.setActionCommand("next");
            itemNext.addActionListener(this);
            itemNext.setFont(font);
            menuConsole.add(itemNext);
            
            MenuItem itemStop = new MenuItem("Stop");
            itemStop.setActionCommand("stop");
            itemStop.addActionListener(this);
            itemStop.setFont(font);
            menuConsole.add(itemStop);

            MenuItem itemExit = new MenuItem("Exit");
            itemExit.setFont(font);
            itemExit.setActionCommand("exit");
            itemExit.addActionListener(this);
            menuConsole.add(itemExit);

            // TrayIcon icon = new TrayIcon(image, "MP3 Player", menuConsole);
            Object trayIcon = Class.forName("java.awt.TrayIcon").
                getConstructor(new Class[] { Image.class, String.class, PopupMenu.class }).
                newInstance(new Object[] { icon, TITLE, menuConsole });

            // SystemTray tray = SystemTray.getSystemTray();
            Object tray = Class.forName("java.awt.SystemTray").
                getMethod("getSystemTray", new Class[0]).
                invoke(null, new Object[0]);

            // trayIcon.addMouseListener(this);
            trayIcon.getClass().
                 getMethod("addMouseListener", new Class[]{MouseListener.class}).
                 invoke(trayIcon, new Object[]{this});
             
             // tray.add(icon);
             tray.getClass().
                getMethod("add", new Class[] { Class.forName("java.awt.TrayIcon") }).
                invoke(tray, new Object[] { trayIcon });
             
             return true;
        } catch (Exception e) {
            return false;
        }
    }
    
    public void actionPerformed(ActionEvent e) {
        String command = e.getActionCommand();
        if ("exit".equals(command)) {
            System.exit(0);
        } else if ("back".equals(command)) {
            File f = files[0];
            if(f != null) {
                File parent = f.getParentFile();
                if(parent == null || parent.getParentFile() == null) {
                    readFiles(File.listRoots());
                } else {
                    readFiles(parent.getParentFile().listFiles());
                }
            }
        } else if ("skip".equals(command)) {
        } else if ("play".equals(command)) {
            File f = getSelectedFile();
            if(f != null) {
                play(f);
            }
        } else if ("stop".equals(command)) {
            if(thread != null) {
                thread.stopPlaying();
            }
//        } else if ("pause".equals(command)) {
//            int todo;
        } else if ("next".equals(command)) {
            if(thread != null) {
                thread.playNext();
            }
//        } else if ("skip".equals(command)) {
//            int todo;
        } else if ("open".equals(command)) {
            open(false);
        }
    }
    
    File getSelectedFile() {
        int index = list.getSelectedIndex();
        if(index < 0 || index >= files.length) {
            return null;
        }
        return files[index];
    }
    
    private void open(final boolean exit) {
        final Frame frame = new Frame(TITLE);
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                if(exit) {
                    System.exit(0);
                } else {
                    frame.dispose();
                }
            }
        });
        if(icon != null) {
            frame.setIconImage(icon);
        }
        frame.setResizable(false);
        frame.setBackground(SystemColor.control);
        
        GridBagLayout layout = new GridBagLayout();
        frame.setLayout(layout);

        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
        c.insets.left = 2;
        c.insets.right = 2;
        c.insets.top = 2;
        c.insets.bottom = 2;
      
        list = new List(7, false) {
            private static final long serialVersionUID = 1L;
            public Dimension getMinimumSize() {
                return new Dimension(200, 200);
            }
            public Dimension getPreferredSize() {
                return getMinimumSize();
            }
        };
        list.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                File f = getSelectedFile();
                if(f != null) {
                    if(f.isDirectory()) {
                        readFiles(f.listFiles());
                    } else if(isMp3(f)) {
                        play(f);
                    }
                }
            }
        });
        readFiles(new File("C:/music").listFiles());

        
        Button back = new Button("Up");
        back.setFocusable(false);
        back.setActionCommand("back");
        back.addActionListener(this);
        back.setFont(font);
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = GridBagConstraints.EAST;
        frame.add(back, c);

        Button play = new Button("> Play >");
        play.setFocusable(false);
        play.setActionCommand("play");
        play.addActionListener(this);
        play.setFont(font);
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = GridBagConstraints.EAST;
        frame.add(play, c);
        
        Button next = new Button(">>");
        next.setFocusable(false);
        next.setActionCommand("next");
        next.addActionListener(this);
        next.setFont(font);
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = GridBagConstraints.EAST;
        frame.add(next, c);

        Button stop = new Button("Stop");
        stop.setFocusable(false);
        stop.setActionCommand("stop");
        stop.addActionListener(this);
        stop.setFont(font);
        c.anchor = GridBagConstraints.EAST;
        c.gridwidth = GridBagConstraints.REMAINDER;
        frame.add(stop, c);

        
        
        
//        Label label = new Label("Playing:", Label.LEFT);
//        label.setFont(font);
        list.setFont(font);
        c.anchor = GridBagConstraints.CENTER;
//        c.gridwidth = GridBagConstraints.EAST;
        c.gridwidth = GridBagConstraints.REMAINDER;
//        frame.add(label, c);

        frame.add(list, c);
        
        //
//        TextField text = new TextField();
//        text.setEditable(false);
//        text.setFont(font);
//        text.setText(web.getURL());
//        text.setFocusable(false);
//        c.anchor = GridBagConstraints.EAST;
//        c.gridwidth = GridBagConstraints.REMAINDER;
//        frame.add(text, c);
//        
//        Label label2 = new Label();
//        c.anchor = GridBagConstraints.WEST;
//        c.gridwidth = GridBagConstraints.EAST;
//        frame.add(label2, c);

        int width = 250, height = 300;
        frame.setSize(width, height);
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        frame.setLocation((screenSize.width - width) / 2, (screenSize.height - height) / 2);
        frame.setVisible(true);

    }

    private void readFiles(File[] f) {
        if(f.length == 0) {
            return;
        }
        // must at least contain one directory or one mp3 file
        ArrayList fileList = new ArrayList();
        for(int i=0; i<f.length; i++) {
            File f2 = f[i];
            if(isMp3(f2) || f2.isDirectory()) {
                fileList.add(f2);
            }
        }
        if(fileList.size() == 0) {
            return;
        }
        list.removeAll();
        this.files = new File[fileList.size()];
        fileList.toArray(files);
        for(int i=0; i<files.length; i++) {
            File f2 = files[i];
            if(isMp3(f2) || f2.isDirectory()) {
                String name = f2.getName().trim();
                if(name.length() == 0) {
                    name = f2.getAbsolutePath();
                }
                list.add(name);
            }
        }
    }
    
    private void play(File f) {
        if(isMp3(f)) {
            if(thread != null) {
                thread.stopPlaying();
                thread = null;
            }
            thread = PlayerThread.startPlaying(f, null);
        } else if(f.isDirectory()) {
            ArrayList files = new ArrayList();
            addAll(files, f);
            if(files.size() > 0) {
                for (int i = 0; i < files.size() ; i++) {
                    Object temp = files.get(i);
                    int x = (int) (Math.random() * files.size());
                    files.set(i, files.get(x));
                    files.set(x, temp);
                }
                if(thread != null) {
                    thread.stopPlaying();
                    thread = null;
                }
                thread = PlayerThread.startPlaying(null, files);
            }
        }
    }
    
    private void addAll(ArrayList arrayList, File file) {
        if (file.isDirectory()) {
            File[] list = file.listFiles();
            for (int i = 0; i < list.length; i++) {
                addAll(arrayList, list[i]);
            }
        } else if(isMp3(file)) {
            arrayList.add(file);
        }
    }
    
    
    private boolean isMp3(File f) {
        return f.getName().toLowerCase().endsWith(".mp3");
    }

    public void mouseClicked(MouseEvent e) {
        if(e.getButton() == MouseEvent.BUTTON1) {
            open(false);
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }
    
    private static byte[] readBytesAndClose(InputStream in, int length) throws IOException {
        try {
            if(length <= 0) {
                length = Integer.MAX_VALUE;
            }
            int block = Math.min(4 * 1024, length);
            ByteArrayOutputStream out=new ByteArrayOutputStream(block);
            byte[] buff=new byte[block];
            while(length > 0) {
                int len = Math.min(block, length);
                len = in.read(buff, 0, len);
                if(len < 0) {
                    break;
                }
                out.write(buff, 0, len);
                length -= len;
            }
            return out.toByteArray();
        } finally {
            in.close();
        }
    }

}