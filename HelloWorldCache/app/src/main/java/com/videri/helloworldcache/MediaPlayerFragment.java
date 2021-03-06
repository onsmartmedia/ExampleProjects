/**
 * MediaPlayerFragment.java
 * Thread Application Sample
 * 1.0.0
 *
 * Copyright 2016 Videri Software, Inc.
 *
 * Unless required by applicable law or agreed to in writing by both parties,
 * this sample software is distributed on an "AS IS" AND "AS AVAILABLE" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */

package com.videri.helloworldcache;

import android.app.Fragment;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.VideoView;


public class MediaPlayerFragment extends Fragment {
    private final String TAG = "MediaPlayerFragment";

    /**
     * A videoview displays video
     */
    private VideoView videoView;
    /**
     * A video file path
     */
    private String videoPath = "";
    /**
     * An videoIndex used to play different  video
     */
    private int currentVideoIndex = 0;
    /**
     * Records video stop position
     */
    private int stopPosition = 0;


    public MediaPlayerFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.media_player_fragment, container, false);


        //get a video file path
        videoPath = "android.resource://" +
                getActivity().getPackageName() + "/" + R.raw.snapchat_bwy;
        //init videoview
        videoView = (VideoView) view.findViewById(R.id.video);
        //assign  the video file path to the videoview
        videoView.setVideoPath(getVideoPath(currentVideoIndex));
        //when video ended, play another video
        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                currentVideoIndex++;
                if(currentVideoIndex > 2)
                    currentVideoIndex = 0;
                videoView.setVideoPath(getVideoPath(currentVideoIndex));
                videoView.start();


            }
        });
        //dismiss video error dialog
        videoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                return true;
            }
        });

        return view;
    }

    /**
     * Return a video path by a given index
     * @param index
     * @return
     */
    private String getVideoPath(int index){
        if(index == 0 )
            return  "android.resource://" +
                    getActivity().getPackageName() + "/" + R.raw.snapchat_bwy;
        else
            return "android.resource://" +
                    getActivity().getPackageName() + "/" + R.raw.hudson_yards_digital_domination;
    }

    /**
     * get the video stop position and pause video
     */
    @Override
    public void onPause() {
        super.onPause();
        stopPosition = videoView.getCurrentPosition();
        videoView.pause();
    }

    /**
     * play the video on last stop position
     */
    @Override
    public void onResume() {
        super.onResume();

        if(videoView != null) {
            videoView.seekTo(stopPosition);
            videoView.start();
        }
    }
}
