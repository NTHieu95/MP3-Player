
package gui;

import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.nimbus.NimbusLookAndFeel;
import javazoom.jlgui.basicplayer.BasicController;
import javazoom.jlgui.basicplayer.BasicPlayerEvent;
import javazoom.jlgui.basicplayer.BasicPlayerListener;
import objects.MP3Player;
import objects.MP3;
import utils.FileUtils;
import utils.MP3PlayerFileFilter;
import utils.SkinUtils;


public class MP3PlayerGui extends javax.swing.JFrame implements BasicPlayerListener {

    //<editor-fold defaultstate="collapsed" desc="Constants">
    private static final String MP3_FILE_EXTENSION = "mp3";
    private static final String MP3_FILE_DESCRIPTION = "File mp3";
    private static final String PLAYLIST_FILE_EXTENSION = "pls";
    private static final String PLAYLIST_FILE_DESCRIPTION = "File Playlist";
    private static final String EMPTY_STRING = "";
    private static final String INPUT_SONG_NAME = "Nhập Tên Bài Hát";
    //</editor-fold>

    private DefaultListModel mp3ListModel = new DefaultListModel();
    private FileFilter mp3FileFilter = new MP3PlayerFileFilter(MP3_FILE_EXTENSION, MP3_FILE_DESCRIPTION);
    private FileFilter playlistFileFilter = new MP3PlayerFileFilter(PLAYLIST_FILE_EXTENSION, PLAYLIST_FILE_DESCRIPTION);
    private MP3Player player = new MP3Player(this);

    //<editor-fold defaultstate="collapsed" desc="Variables of songs">
    private long secondsAmount; // Second passed
    private long duration; // Duration
    private int bytesLen; // Song's size in bytes
    private double posValue = 0.0; // Scroll position
    // Whether the song slider is moved by dragging (or play) - used during rewind
    private boolean movingFromJump = false;
    private boolean moveAutomatic = false;// While playing the song slider moves, moveAutomatic = true
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Player event BasicPlayerListener">
    @Override
    public void opened(Object o, Map map) {
        
            /*Set s = map.keySet();
            Object[] str =  s.toArray();
            for(int i=0;i<str.length;i++){
                System.out.println(str[i]);
            }*/
        
        


        // determine the length of the song file size
        duration = (long) Math.round((((Long) map.get("duration")).longValue()) / 1000000);
        bytesLen = (int) Math.round(((Integer) map.get("mp3.length.bytes")).intValue());

        // If mp3 file has tag name: take it. Else, take the filename
        String songName = map.get("title") != null ? map.get("title").toString() : FileUtils.getFileNameWithoutExtension(new File(o.toString()).getName());
        String songArtist = map.get("author") !=null ? map.get("author").toString() : FileUtils.getFileNameWithoutExtension(new File(o.toString()).getName());

        String songAlbum = map.get("album") !=null ? map.get("album").toString() : FileUtils.getFileNameWithoutExtension(new File(o.toString()).getName());
        // If the name is too long, shorten it
        if (songName.length() > 30) {
            songName = songName.substring(0, 30) + "...";
        }

        labelSongName.setText("<html>"+"Title: "+songName+"<br>"+"Artist: "+songArtist+"<br>"+"Album: "+songAlbum+"</html>");

    }

    @Override
    public void progress(int bytesread, long microseconds, byte[] pcmdata, Map properties) {

        float progress = -1.0f;

        if ((bytesread > 0) && ((duration > 0))) {
            progress = bytesread * 1.0f / bytesLen * 1.0f;
        }


        // How many seconds have passed
        secondsAmount = (long) (duration * progress);

        if (duration != 0) {
            if (movingFromJump == false) {
                slideProgress.setValue(((int) Math.round(secondsAmount * 1000 / duration)));

            }
        }
    }

    @Override
    public void stateUpdated(BasicPlayerEvent bpe) {
        int state = bpe.getCode();

        if (state == BasicPlayerEvent.PLAYING) {
            movingFromJump = false;
        } else if (state == BasicPlayerEvent.SEEKING) {
            movingFromJump = true;
        } else if (state == BasicPlayerEvent.EOM) {
            if (selectNextSong()) {
                playFile();
            }
        }


    }

    @Override
    public void setController(BasicController bc) {
    }
    //</editor-fold>

    
    public MP3PlayerGui() {
        initComponents();

    }

    private void playFile() {
        int[] indexPlayList = lstPlayList.getSelectedIndices();// Select index songs
        if (indexPlayList.length > 0) {// If lenght of playlist > 0
            MP3 mp3 = (MP3) mp3ListModel.getElementAt(indexPlayList[0]);// start playing from 1st song 
            player.play(mp3.getPath());
            player.setVolume(slideVolume.getValue(), slideVolume.getMaximum());
        }

    }

    private boolean selectPrevSong() {
        int nextIndex = lstPlayList.getSelectedIndex() - 1;
        if (nextIndex >= 0) {// If current index is still on the playlist
            lstPlayList.setSelectedIndex(nextIndex);
            return true;
        }

        return false;
    }

    private boolean selectNextSong() {
        int nextIndex = lstPlayList.getSelectedIndex() + 1;
        if (nextIndex <= lstPlayList.getModel().getSize() - 1) {// If current index is still on the playlist
            lstPlayList.setSelectedIndex(nextIndex);
            return true;
        }
        return false;
    }

    private void searchSong() {
        String searchStr = txtSearch.getText();

        // If search txt is empty, return
        if (searchStr == null || searchStr.trim().equals(EMPTY_STRING)) {
            return;
        }

        // All found indexes will be highlight
        ArrayList<Integer> mp3FindedIndexes = new ArrayList<Integer>();

        // Find the song with inputed string in current playlist
        for (int i = 0; i < mp3ListModel.size(); i++) {
            MP3 mp3 = (MP3) mp3ListModel.getElementAt(i);
            // Search in file name, including lower-upper case
            if (mp3.getName().toUpperCase().contains(searchStr.toUpperCase())) {
                mp3FindedIndexes.add(i);// add to collection
            }
        }

        // The collection stored in array
        int[] selectIndexes = new int[mp3FindedIndexes.size()];

        if (selectIndexes.length == 0) {// If not found
            JOptionPane.showMessageDialog(this, "Không tìm thấy \'" + searchStr + "\'");
            txtSearch.requestFocus();
            txtSearch.selectAll();
            return;
        }

        // Convert collection into array (JList ony works on array)
        for (int i = 0; i < selectIndexes.length; i++) {
            selectIndexes[i] = mp3FindedIndexes.get(i).intValue();
        }

        // Choose the found song(s)
        lstPlayList.setSelectedIndices(selectIndexes);
    }

    private void processSeek(double bytes) {
        try {
            long skipBytes = (long) Math.round(((Integer) bytesLen).intValue() * bytes);
            player.jump(skipBytes);
        } catch (Exception e) {
            e.printStackTrace();
            movingFromJump = false;
        }

    }

    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        fileChooser = new javax.swing.JFileChooser();
        popupPlaylist = new javax.swing.JPopupMenu();
        popmenuAddSong = new javax.swing.JMenuItem();
        popmenuDeleteSong = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        popmenuOpenPlaylist = new javax.swing.JMenuItem();
        popmenuClearPlaylist = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        popmenuPlay = new javax.swing.JMenuItem();
        popmenuStop = new javax.swing.JMenuItem();
        popmenuPause = new javax.swing.JMenuItem();
        panelMain = new javax.swing.JPanel();
        btnStopSong = new javax.swing.JButton();
        btnPauseSong = new javax.swing.JButton();
        btnPlaySong = new javax.swing.JButton();
        btnNextSong = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        lstPlayList = new javax.swing.JList();
        slideVolume = new javax.swing.JSlider();
        tglbtnVolume = new javax.swing.JToggleButton();
        btnPrevSong = new javax.swing.JButton();
        btnAddSong = new javax.swing.JButton();
        btnDeleteSong = new javax.swing.JButton();
        btnSelectNext = new javax.swing.JButton();
        jSeparator2 = new javax.swing.JSeparator();
        btnSelectPrev = new javax.swing.JButton();
        slideProgress = new javax.swing.JSlider();
        labelSongName = new javax.swing.JLabel();
        panelSearch = new javax.swing.JPanel();
        btnSearch = new javax.swing.JButton();
        txtSearch = new javax.swing.JTextField();
        jMenuBar1 = new javax.swing.JMenuBar();
        menuFile = new javax.swing.JMenu();
        menuOpenPlaylist = new javax.swing.JMenuItem();
        menuSavePlaylist = new javax.swing.JMenuItem();
        menuSeparator = new javax.swing.JPopupMenu.Separator();
        menuExit = new javax.swing.JMenuItem();
        menuPrefs = new javax.swing.JMenu();
        menuChangeSkin = new javax.swing.JMenu();
        menuSkin1 = new javax.swing.JMenuItem();
        menuSkin2 = new javax.swing.JMenuItem();

        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setDialogTitle("Chọn File");
        fileChooser.setMultiSelectionEnabled(true);

        popmenuAddSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/plus_16.png"))); // NOI18N
        popmenuAddSong.setText("Thêm Bài Hát");
        popmenuAddSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuAddSongActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuAddSong);

        popmenuDeleteSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/remove_icon.png"))); // NOI18N
        popmenuDeleteSong.setText("Xóa Bài Hát");
        popmenuDeleteSong.setToolTipText("");
        popmenuDeleteSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuDeleteSongActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuDeleteSong);
        popupPlaylist.add(jSeparator1);

        popmenuOpenPlaylist.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/open-icon.png"))); // NOI18N
        popmenuOpenPlaylist.setText("Mở Playlist");
        popmenuOpenPlaylist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuOpenPlaylistActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuOpenPlaylist);

        popmenuClearPlaylist.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/clear.png"))); // NOI18N
        popmenuClearPlaylist.setText("Clear Playlist");
        popmenuClearPlaylist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuClearPlaylistActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuClearPlaylist);
        popupPlaylist.add(jSeparator3);

        popmenuPlay.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/Play.png"))); // NOI18N
        popmenuPlay.setText("Bật");
        popmenuPlay.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuPlayActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuPlay);

        popmenuStop.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/stop-red-icon.png"))); // NOI18N
        popmenuStop.setText("Dừng");
        popmenuStop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuStopActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuStop);

        popmenuPause.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/Pause-icon.png"))); // NOI18N
        popmenuPause.setText("Tạm Dừng");
        popmenuPause.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                popmenuPauseActionPerformed(evt);
            }
        });
        popupPlaylist.add(popmenuPause);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("MP3 Player");
        setResizable(false);

        panelMain.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        btnStopSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/stop-red-icon.png"))); // NOI18N
        btnStopSong.setToolTipText("Dừng");
        btnStopSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnStopSongActionPerformed(evt);
            }
        });

        btnPauseSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/Pause-icon.png"))); // NOI18N
        btnPauseSong.setToolTipText("Tạm Dừng");
        btnPauseSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPauseSongActionPerformed(evt);
            }
        });

        btnPlaySong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/Play.png"))); // NOI18N
        btnPlaySong.setToolTipText("Bật");
        btnPlaySong.setName("btnPlay"); // NOI18N
        btnPlaySong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPlaySongActionPerformed(evt);
            }
        });

        btnNextSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/next-icon.png"))); // NOI18N
        btnNextSong.setToolTipText("Bài Hát Tiếp Theo");
        btnNextSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNextSongActionPerformed(evt);
            }
        });

        lstPlayList.setModel(mp3ListModel);
        lstPlayList.setToolTipText("Danh Sách Bài Hát");
        lstPlayList.setComponentPopupMenu(popupPlaylist);
        lstPlayList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lstPlayListMouseClicked(evt);
            }
        });
        lstPlayList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                lstPlayListKeyPressed(evt);
            }
        });
        jScrollPane2.setViewportView(lstPlayList);

        slideVolume.setMaximum(200);
        slideVolume.setMinorTickSpacing(5);
        slideVolume.setSnapToTicks(true);
        slideVolume.setToolTipText("Chỉnh Âm Lượng");
        slideVolume.setValue(100);
        slideVolume.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                slideVolumeStateChanged(evt);
            }
        });

        tglbtnVolume.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/speaker.png"))); // NOI18N
        tglbtnVolume.setToolTipText("Tắt Âm");
        tglbtnVolume.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/images/mute.png"))); // NOI18N
        tglbtnVolume.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                tglbtnVolumeActionPerformed(evt);
            }
        });

        btnPrevSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/prev-icon.png"))); // NOI18N
        btnPrevSong.setToolTipText("Bài Hát Trước Đó");
        btnPrevSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrevSongActionPerformed(evt);
            }
        });

        btnAddSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/plus_16.png"))); // NOI18N
        btnAddSong.setToolTipText("Thêm Bài Hát");
        btnAddSong.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnAddSong.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        btnAddSong.setName("btnAddSong"); // NOI18N
        btnAddSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnAddSongActionPerformed(evt);
            }
        });

        btnDeleteSong.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/remove_icon.png"))); // NOI18N
        btnDeleteSong.setToolTipText("Xóa Bài Hát");
        btnDeleteSong.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnDeleteSong.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        btnDeleteSong.setName("btnAddSong"); // NOI18N
        btnDeleteSong.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnDeleteSongActionPerformed(evt);
            }
        });

        btnSelectNext.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/arrow-down-icon.png"))); // NOI18N
        btnSelectNext.setToolTipText("Bài Hát Tiếp Theo");
        btnSelectNext.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnSelectNext.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        btnSelectNext.setName("btnAddSong"); // NOI18N
        btnSelectNext.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSelectNextActionPerformed(evt);
            }
        });

        jSeparator2.setOrientation(javax.swing.SwingConstants.VERTICAL);

        btnSelectPrev.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/arrow-up-icon.png"))); // NOI18N
        btnSelectPrev.setToolTipText("Bài Hát Trước Đó");
        btnSelectPrev.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        btnSelectPrev.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        btnSelectPrev.setName("btnAddSong"); // NOI18N
        btnSelectPrev.setVerticalTextPosition(javax.swing.SwingConstants.TOP);
        btnSelectPrev.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSelectPrevActionPerformed(evt);
            }
        });

        slideProgress.setMaximum(1000);
        slideProgress.setMinorTickSpacing(1);
        slideProgress.setSnapToTicks(true);
        slideProgress.setToolTipText("Thanh Thời Lượng");
        slideProgress.setValue(0);
        slideProgress.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                slideProgressStateChanged(evt);
            }
        });

        labelSongName.setText("...");
        labelSongName.setName(""); // NOI18N

        javax.swing.GroupLayout panelMainLayout = new javax.swing.GroupLayout(panelMain);
        panelMain.setLayout(panelMainLayout);
        panelMainLayout.setHorizontalGroup(
            panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMainLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(panelMainLayout.createSequentialGroup()
                        .addComponent(btnAddSong, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnDeleteSong)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 13, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(37, 37, 37)
                        .addComponent(btnSelectNext, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnSelectPrev, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(slideProgress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(panelMainLayout.createSequentialGroup()
                        .addComponent(tglbtnVolume, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(panelMainLayout.createSequentialGroup()
                                .addComponent(btnPrevSong, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnPlaySong, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnPauseSong, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnStopSong, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(btnNextSong, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(panelMainLayout.createSequentialGroup()
                                .addGap(28, 28, 28)
                                .addComponent(slideVolume, javax.swing.GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE))))
                    .addGroup(panelMainLayout.createSequentialGroup()
                        .addComponent(labelSongName)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        panelMainLayout.setVerticalGroup(
            panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelMainLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(btnSelectNext, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jSeparator2)
                        .addComponent(btnSelectPrev, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(btnAddSong, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnDeleteSong, javax.swing.GroupLayout.Alignment.TRAILING))
                .addGap(18, 18, 18)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 255, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(labelSongName)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(slideProgress, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(slideVolume, javax.swing.GroupLayout.PREFERRED_SIZE, 23, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(tglbtnVolume))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(panelMainLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(btnPlaySong, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnPauseSong, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnStopSong, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnNextSong, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(btnPrevSong, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(68, 68, 68))
        );

        btnDeleteSong.getAccessibleContext().setAccessibleDescription("Xóa Bài Hát");

        panelSearch.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        btnSearch.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/search_16.png"))); // NOI18N
        btnSearch.setText("Tìm");
        btnSearch.setToolTipText("Tìm Tên Bài Hát");
        btnSearch.setActionCommand("search");
        btnSearch.setName("btnSearch"); // NOI18N
        btnSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSearchActionPerformed(evt);
            }
        });

        txtSearch.setFont(new java.awt.Font("Tahoma", 2, 11)); // NOI18N
        txtSearch.setForeground(new java.awt.Color(153, 153, 153));
        txtSearch.setText("Nhập tên bài hát");
        txtSearch.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txtSearchFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtSearchFocusLost(evt);
            }
        });
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtSearchKeyPressed(evt);
            }
        });

        javax.swing.GroupLayout panelSearchLayout = new javax.swing.GroupLayout(panelSearch);
        panelSearch.setLayout(panelSearchLayout);
        panelSearchLayout.setHorizontalGroup(
            panelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSearchLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(txtSearch)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSearch)
                .addContainerGap())
        );
        panelSearchLayout.setVerticalGroup(
            panelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(panelSearchLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(panelSearchLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnSearch))
                .addContainerGap())
        );

        txtSearch.getAccessibleContext().setAccessibleName("");
        txtSearch.getAccessibleContext().setAccessibleDescription("");

        menuFile.setText("File");

        menuOpenPlaylist.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/open-icon.png"))); // NOI18N
        menuOpenPlaylist.setText("Mở Playlist");
        menuOpenPlaylist.setName(""); // NOI18N
        menuOpenPlaylist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuOpenPlaylistActionPerformed(evt);
            }
        });
        menuFile.add(menuOpenPlaylist);

        menuSavePlaylist.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/save_16.png"))); // NOI18N
        menuSavePlaylist.setText("Lưu Playlist");
        menuSavePlaylist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSavePlaylistActionPerformed(evt);
            }
        });
        menuFile.add(menuSavePlaylist);
        menuFile.add(menuSeparator);

        menuExit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/exit.png"))); // NOI18N
        menuExit.setText("Thoát");
        menuExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuExitActionPerformed(evt);
            }
        });
        menuFile.add(menuExit);

        jMenuBar1.add(menuFile);

        menuPrefs.setText("Service");

        menuChangeSkin.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/gear_16.png"))); // NOI18N
        menuChangeSkin.setText("Giao diện");

        menuSkin1.setText("Skin 1");
        menuSkin1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSkin1ActionPerformed(evt);
            }
        });
        menuChangeSkin.add(menuSkin1);

        menuSkin2.setText("Skin 2");
        menuSkin2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSkin2ActionPerformed(evt);
            }
        });
        menuChangeSkin.add(menuSkin2);

        menuPrefs.add(menuChangeSkin);

        jMenuBar1.add(menuPrefs);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(panelSearch, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(panelMain, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(panelSearch, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(panelMain, javax.swing.GroupLayout.PREFERRED_SIZE, 520, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(22, Short.MAX_VALUE))
        );

        getAccessibleContext().setAccessibleName("MP3 Player");

        setSize(new java.awt.Dimension(366, 660));
        setLocationRelativeTo(null);
    }// </editor-fold>

    private void btnPlaySongActionPerformed(java.awt.event.ActionEvent evt) {
        playFile();
    }

    private void btnStopSongActionPerformed(java.awt.event.ActionEvent evt) {
        player.stop();
    }

    private void btnPauseSongActionPerformed(java.awt.event.ActionEvent evt) {
        player.pause();
    }

    private void slideVolumeStateChanged(javax.swing.event.ChangeEvent evt) {
        player.setVolume(slideVolume.getValue(), slideVolume.getMaximum());

        if (slideVolume.getValue() == 0) {
            tglbtnVolume.setSelected(true);
        } else {
            tglbtnVolume.setSelected(false);
        }
    }

    private void btnNextSongActionPerformed(java.awt.event.ActionEvent evt) {
        if (selectNextSong()) {
            playFile();
        }
    }

    private void btnPrevSongActionPerformed(java.awt.event.ActionEvent evt) {
        if (selectPrevSong()) {
            playFile();
        }
    }

    private void btnSelectPrevActionPerformed(java.awt.event.ActionEvent evt) {
        selectPrevSong();
    }

    private void btnSelectNextActionPerformed(java.awt.event.ActionEvent evt) {
        selectNextSong();
    }

    private void btnAddSongActionPerformed(java.awt.event.ActionEvent evt) {
        FileUtils.addFileFilter(fileChooser, mp3FileFilter);
        int result = fileChooser.showOpenDialog(this);// a file is selected or not

        if (result == JFileChooser.APPROVE_OPTION) {// If OK / Yes

            File[] selectedFiles = fileChooser.getSelectedFiles();
            // Take all the selected files into playlist
            for (File file : selectedFiles) {
                MP3 mp3 = new MP3(file.getName(), file.getPath());

                // if the song's already on the playlist, do not add
                if (!mp3ListModel.contains(mp3)) {
                    mp3ListModel.addElement(mp3);
                }
            }

        }
    }

    private void btnDeleteSongActionPerformed(java.awt.event.ActionEvent evt) {
        int[] indexPlayList = lstPlayList.getSelectedIndices();// Get the selected song(s)
        if (indexPlayList.length > 0) {// if you have choosen at least 1 song
            ArrayList<MP3> mp3ListForRemove = new ArrayList<MP3>();// Save the deleting files list to another array
            for (int i = 0; i < indexPlayList.length; i++) {// Delete all the selected file(s)
                MP3 mp3 = (MP3) mp3ListModel.getElementAt(indexPlayList[i]);
                mp3ListForRemove.add(mp3);
            }

            // Remove from playlist
            for (MP3 mp3 : mp3ListForRemove) {
                mp3ListModel.removeElement(mp3);
            }

        }
    }

    private void btnSearchActionPerformed(java.awt.event.ActionEvent evt) {
        searchSong();

    }

    private void txtSearchFocusGained(java.awt.event.FocusEvent evt) {
        if (txtSearch.getText().equals(INPUT_SONG_NAME)) {
            txtSearch.setText(EMPTY_STRING);
        }
    }

    private void txtSearchFocusLost(java.awt.event.FocusEvent evt) {
        if (txtSearch.getText().trim().equals(EMPTY_STRING)) {
            txtSearch.setText(INPUT_SONG_NAME);
        }
    }

    private void menuSavePlaylistActionPerformed(java.awt.event.ActionEvent evt) {
        FileUtils.addFileFilter(fileChooser, playlistFileFilter);
        int result = fileChooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {// If select OK / Yes
            File selectedFile = fileChooser.getSelectedFile();
            if (selectedFile.exists()) {//If file already existed

                int resultOvveride = JOptionPane.showConfirmDialog(this, "Bạn có muốn ghi đè", "File đã tồn tại", JOptionPane.YES_NO_CANCEL_OPTION);
                switch (resultOvveride) {
                    case JOptionPane.NO_OPTION:
                        menuSavePlaylistActionPerformed(evt);//Re-open the file, save
                        return;
                    case JOptionPane.CANCEL_OPTION:
                        fileChooser.cancelSelection();
                        return;
                }
                fileChooser.approveSelection();
            }

            String fileExtension = FileUtils.getFileExtension(selectedFile);

            // Adding .pls to file name
            String fileNameForSave = (fileExtension != null && fileExtension.equals(PLAYLIST_FILE_EXTENSION)) ? selectedFile.getPath() : selectedFile.getPath() + "." + PLAYLIST_FILE_EXTENSION;

            FileUtils.serialize(mp3ListModel, fileNameForSave);
        }

    }

    private void menuOpenPlaylistActionPerformed(java.awt.event.ActionEvent evt) {
        FileUtils.addFileFilter(fileChooser, playlistFileFilter);
        int result = fileChooser.showOpenDialog(this);//Result: file selected or not


        if (result == JFileChooser.APPROVE_OPTION) {// If select OK / Yes
            File selectedFile = fileChooser.getSelectedFile();//
            DefaultListModel mp3ListModel = (DefaultListModel) FileUtils.deserialize(selectedFile.getPath());
            this.mp3ListModel = mp3ListModel;
            lstPlayList.setModel(mp3ListModel);
        }


    }

    private void menuExitActionPerformed(java.awt.event.ActionEvent evt) {
        System.exit(0);
    }

    private void menuSkin1ActionPerformed(java.awt.event.ActionEvent evt) {
        SkinUtils.changeSkin(this, UIManager.getSystemLookAndFeelClassName());
        fileChooser.updateUI();
    }

    private void menuSkin2ActionPerformed(java.awt.event.ActionEvent evt) {
        SkinUtils.changeSkin(this, new NimbusLookAndFeel());
        fileChooser.updateUI();
    }

    private void lstPlayListMouseClicked(java.awt.event.MouseEvent evt) {

        // If double-clicked:
        if (evt.getModifiers() == InputEvent.BUTTON1_MASK && evt.getClickCount() == 2) {
            playFile();
        }
    }
    private int currentVolumeValue;
    private void tglbtnVolumeActionPerformed(java.awt.event.ActionEvent evt) {

        if (tglbtnVolume.isSelected()) {
            currentVolumeValue = slideVolume.getValue();
            slideVolume.setValue(0);
        } else {
            slideVolume.setValue(currentVolumeValue);
        }
    }

    private void popmenuAddSongActionPerformed(java.awt.event.ActionEvent evt) {
        btnAddSongActionPerformed(evt);
    }

    private void popmenuDeleteSongActionPerformed(java.awt.event.ActionEvent evt) {
        btnDeleteSongActionPerformed(evt);
    }

    private void popmenuOpenPlaylistActionPerformed(java.awt.event.ActionEvent evt) {
        menuOpenPlaylistActionPerformed(evt);
    }

    private void popmenuClearPlaylistActionPerformed(java.awt.event.ActionEvent evt) {
        mp3ListModel.clear();
    }

    private void slideProgressStateChanged(javax.swing.event.ChangeEvent evt) {

        if (slideProgress.getValueIsAdjusting() == false) {
            if (moveAutomatic == true) {
                moveAutomatic = false;
                posValue = slideProgress.getValue() * 1.0 / 1000;
                processSeek(posValue);
            }
        } else {
            moveAutomatic = true;
            movingFromJump = true;
        }

    }

    private void popmenuPlayActionPerformed(java.awt.event.ActionEvent evt) {
        btnPlaySongActionPerformed(evt);
    }

    private void popmenuStopActionPerformed(java.awt.event.ActionEvent evt) {
        btnStopSongActionPerformed(evt);
    }

    private void popmenuPauseActionPerformed(java.awt.event.ActionEvent evt) {
        btnPauseSongActionPerformed(evt);
    }

    private void lstPlayListKeyPressed(java.awt.event.KeyEvent evt) {
        int key = evt.getKeyCode();
        if (key == KeyEvent.VK_ENTER) {
            playFile();
        }
    }

    private void txtSearchKeyPressed(java.awt.event.KeyEvent evt) {
        int key = evt.getKeyCode();
        if (key == KeyEvent.VK_ENTER) {
            searchSong();
        }
    }

    
    public static void main(String args[]) {
        
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;


                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(MP3PlayerGui.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(MP3PlayerGui.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(MP3PlayerGui.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(MP3PlayerGui.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(new Runnable() {

            public void run() {
                new MP3PlayerGui().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify
    private javax.swing.JButton btnAddSong;
    private javax.swing.JButton btnDeleteSong;
    private javax.swing.JButton btnNextSong;
    private javax.swing.JButton btnPauseSong;
    private javax.swing.JButton btnPlaySong;
    private javax.swing.JButton btnPrevSong;
    private javax.swing.JButton btnSearch;
    private javax.swing.JButton btnSelectNext;
    private javax.swing.JButton btnSelectPrev;
    private javax.swing.JButton btnStopSong;
    private javax.swing.JFileChooser fileChooser;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JLabel labelSongName;
    private javax.swing.JList lstPlayList;
    private javax.swing.JMenu menuChangeSkin;
    private javax.swing.JMenuItem menuExit;
    private javax.swing.JMenu menuFile;
    private javax.swing.JMenuItem menuOpenPlaylist;
    private javax.swing.JMenu menuPrefs;
    private javax.swing.JMenuItem menuSavePlaylist;
    private javax.swing.JPopupMenu.Separator menuSeparator;
    private javax.swing.JMenuItem menuSkin1;
    private javax.swing.JMenuItem menuSkin2;
    private javax.swing.JPanel panelMain;
    private javax.swing.JPanel panelSearch;
    private javax.swing.JMenuItem popmenuAddSong;
    private javax.swing.JMenuItem popmenuClearPlaylist;
    private javax.swing.JMenuItem popmenuDeleteSong;
    private javax.swing.JMenuItem popmenuOpenPlaylist;
    private javax.swing.JMenuItem popmenuPause;
    private javax.swing.JMenuItem popmenuPlay;
    private javax.swing.JMenuItem popmenuStop;
    private javax.swing.JPopupMenu popupPlaylist;
    public static javax.swing.JSlider slideProgress;
    private javax.swing.JSlider slideVolume;
    private javax.swing.JToggleButton tglbtnVolume;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration
}
