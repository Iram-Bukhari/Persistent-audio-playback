package com.example.exoplayerdemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.TabLayout;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.DefaultRenderersFactory;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.RenderersFactory;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import static com.example.exoplayerdemo.AudioBackService.ACTION_START_FOREGROUND_SERVICE;

public class MainActivity extends AppCompatActivity implements Player.EventListener, ServiceCallbacks {

    CardView card_with_media;
    CardView card_without_media;

    CardView card_with_video;
    Button playAudio;
    View bottomSheet;
    BottomSheetBehavior behavior;
    ImageView playPauseButton;
    ImageView bottomSheetIcon;
    //Player
    private Handler mainHandler;
    private RenderersFactory renderersFactory;
    private BandwidthMeter bandwidthMeter;
    private LoadControl loadControl;
    private DataSource.Factory dataSourceFactory;
    private ExtractorsFactory extractorsFactory;
    private MediaSource mediaSource;
    private TrackSelection.Factory trackSelectionFactory;
    private static SimpleExoPlayer player;
    private final String streamImage = "https://i.cbc.ca/1.3001651.1507763461!/httpImage/image.jpg_gen/derivatives/16x9_620/image.jpg";
    private final String streamUrl = "http://mp3.cbc.ca/news/CBC_News_VMS/636/63/Money_Worries_Audio_L1_corrected.mp3";//"http://bbcwssc.ic.llnwd.net/stream/bbcwssc_mp1_ws-einws"; //bbc world service url
    private TrackSelector trackSelector;
    private PlayerView playerView;

    private ImageView audioHeadline , videoHeadline;
    private AudioBackService myService;
    private String videoUrl = "https://content.jwplatform.com/manifests/yp34SRmf.m3u8";


    //** Callbacks for service binding, passed to bindService() *//*
    private ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            // cast the IBinder and get MyService instance
            AudioBackService.LocalBinder binder = (AudioBackService.LocalBinder) service;
            myService = binder.getService();

            myService.setCallbacks(MainActivity.this); // register
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, AudioBackService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getSupportActionBar().setLogo(R.drawable.ic_logo_cbcnews);
        getSupportActionBar().setTitle("");
        getSupportActionBar().setDisplayUseLogoEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        setContentView(R.layout.activity_main);
        card_with_media = findViewById(R.id.card_view_withmedia);
        card_without_media = findViewById(R.id.card_view_withoutmedia);
        card_with_video = findViewById(R.id.card_view_withvideo);

        audioHeadline = findViewById(R.id.ivHeadlineImage);
        videoHeadline = findViewById(R.id.ivHeadlineImage3);
        playAudio = findViewById(R.id.playAudioButton);
        bottomSheet = findViewById(R.id.design_bottom_sheet);
        behavior = BottomSheetBehavior.from(bottomSheet);
        playerView = bottomSheet.findViewById(R.id.player_view);
        playPauseButton = bottomSheet.findViewById(R.id.imagePlayPause);
        bottomSheetIcon = bottomSheet.findViewById(R.id.imageView);

        card_with_media.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player == null) {
                    startPlaying();
                }
                Intent startIntent = new Intent(getApplicationContext(), AudioBackService.class);
                startIntent.setAction(ACTION_START_FOREGROUND_SERVICE);
                startService(startIntent);
                if (behavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                    behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                } else {
                    behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }

            }
        });
        card_without_media.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent i = new Intent(MainActivity.this,StoryActivity.class);
                startActivity(i);
            }
        });
        card_with_video.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent =new Intent(MainActivity.this, VideoActivity.class);
                intent.putExtra(VideoActivity.ARG_VIDEO_URL, videoUrl);
                startActivity(intent);
            }
        });


        playPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (player != null) {
                    if (player.getPlayWhenReady() == true && player.getPlaybackState() == Player.STATE_READY) {
                        player.setPlayWhenReady(false);
                    } else {
                        player.setPlayWhenReady(true);
                    }
                } else {
                    startPlaying();
                }
            }
        });
        behavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
            @Override
            public void onStateChanged(@NonNull View bottomSheet, int newState) {
                switch (newState) {
                    case BottomSheetBehavior.STATE_DRAGGING:
                        break;
                    case BottomSheetBehavior.STATE_SETTLING:
                        break;
                    case BottomSheetBehavior.STATE_EXPANDED:
                        playPauseButton.setVisibility(View.INVISIBLE);
                        if (player == null) {
                            startPlaying();
                        } else {
                            player.setPlayWhenReady(true);
                        }
                        break;
                    case BottomSheetBehavior.STATE_COLLAPSED:
                        playPauseButton.setVisibility(View.VISIBLE);

                        break;
                    case BottomSheetBehavior.STATE_HIDDEN:
                        break;
                }
            }

            @Override
            public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            }
        });
        GlideApp.with(this)
                .asBitmap()
                .load("https://i.cbc.ca/1.4813493.1536283501!/httpImage/image.jpg_gen/derivatives/16x9_300/image.jpg")
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.AUTOMATIC))
                .into(new SimpleTarget<Bitmap>(300, 168) {
                    @Override
                    public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                        videoHeadline.setImageBitmap(bitmap);
                    }
                });
    }

    public void startPlaying() {
        renderersFactory = new DefaultRenderersFactory(getApplicationContext());
        bandwidthMeter = new DefaultBandwidthMeter();
        trackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
        trackSelector = new DefaultTrackSelector(trackSelectionFactory);
        loadControl = new DefaultLoadControl();
        player = ExoPlayerFactory.newSimpleInstance(renderersFactory, trackSelector, loadControl);
        player.addListener(this);

        dataSourceFactory = new DefaultDataSourceFactory(getApplicationContext(), "ExoplayerDemo");
        extractorsFactory = new DefaultExtractorsFactory();
        mainHandler = new Handler();
        mediaSource = new ExtractorMediaSource(Uri.parse(streamUrl),
                dataSourceFactory,
                extractorsFactory,
                mainHandler,
                null);

        playerView.showController();
        playerView.setPlayer(player);
        GlideApp.with(this)
                .asBitmap()
                .load(streamImage)
                .apply(RequestOptions.diskCacheStrategyOf(DiskCacheStrategy.AUTOMATIC))
                .into(new SimpleTarget<Bitmap>(620, 349) {
                    @Override
                    public void onResourceReady(Bitmap bitmap, Transition<? super Bitmap> transition) {
                        playerView.setDefaultArtwork(bitmap);
                        audioHeadline.setImageBitmap(bitmap);
                    }
                });


        player.prepare(mediaSource);
        player.setPlayWhenReady(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onTimelineChanged(Timeline timeline, Object manifest, int reason) {

    }

    @Override
    public void onTracksChanged(TrackGroupArray trackGroups, TrackSelectionArray trackSelections) {

    }

    @Override
    public void onLoadingChanged(boolean isLoading) {

    }

    @Override
    public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
        long cur_pos = 0;
        if ((playbackState == Player.STATE_READY) && playWhenReady) {
            cur_pos = player.getCurrentPosition();
            myService.updateNotification(PlaybackStateCompat.STATE_PLAYING, cur_pos);
            playPauseButton.setImageResource(R.drawable.ic_pause);

            Log.d("onPlayerStateChanged:", "PLAYING");
        } else if ((playbackState == Player.STATE_READY)) {

            cur_pos = player.getCurrentPosition();
            myService.updateNotification(PlaybackStateCompat.STATE_PAUSED, cur_pos);
            playPauseButton.setImageResource(R.drawable.ic_play_arrow);

            Log.d("onPlayerStateChanged:", "PAUSED");
        }

    }

    @Override
    public void onRepeatModeChanged(int repeatMode) {

    }

    @Override
    public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {

    }

    @Override
    public void onPlayerError(ExoPlaybackException error) {

    }

    @Override
    public void onPositionDiscontinuity(int reason) {

    }

    @Override
    public void onPlaybackParametersChanged(PlaybackParameters playbackParameters) {

    }

    @Override
    public void onSeekProcessed() {

    }

    @Override
    public void play() {
        player.setPlayWhenReady(true);
    }

    @Override
    public void pause() {
        player.setPlayWhenReady(false);
    }

    @Override
    public void seekTo() {
        player.seekTo(0);
    }
    @Override
    public void fast() {

        player.seekTo(player.getCurrentPosition()+15000);
    }

    @Override
    public void rewind() {
        long rewind = player.getCurrentPosition()-15000;
        if(rewind>0)
        player.seekTo(player.getCurrentPosition()-15000);
        else
            player.seekTo(0);
    }

    private void releasePlayer() {
        player.stop();
        player.release();
        player = null;
        myService.stopForegroundService();

    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
        moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releasePlayer();

    }
}
