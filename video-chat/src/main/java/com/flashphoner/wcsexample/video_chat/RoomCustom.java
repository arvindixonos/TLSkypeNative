package com.flashphoner.wcsexample.video_chat;

//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

import com.flashphoner.fpwcsapi.session.Stream;
import com.flashphoner.fpwcsapi.session.StreamOptions;

import org.webrtc.SurfaceViewRenderer;

public class RoomCustom {
    private Stream stream;

    public Stream publish(SurfaceViewRenderer renderer) {
        if (this.stream == null) {
            StreamOptions streamOptions = new StreamOptions("THIS IS STREAM OPTIONS");
            streamOptions.setRenderer(renderer);
            streamOptions.setCustom("name", "THIS IS NAME");
            streamOptions.getConstraints().updateVideo(false);
            //this.stream = this.roomManager.getSession().createStream(streamOptions);
            this.stream.publish();
        }

        return this.stream;
    }
}

