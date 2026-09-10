package com.resolveiq.analysis.adapter.out.evidence;

import com.resolveiq.analysis.application.service.evidence.EvidenceExtractionModels.*;
import com.resolveiq.analysis.application.service.evidence.OcrPort;
import com.resolveiq.analysis.application.service.evidence.VideoFrameSamplingPort;
import com.resolveiq.analysis.domain.model.evidence.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
public class FfmpegVideoAnalysisAdapter implements VideoFrameSamplingPort {
    private final OcrPort ocr;
    private final String ffmpeg;
    private final String ffprobe;

    public FfmpegVideoAnalysisAdapter(OcrPort ocr,
        @Value("${resolveiq.evidence.tools.ffmpeg:ffmpeg}") String ffmpeg,
        @Value("${resolveiq.evidence.tools.ffprobe:ffprobe}") String ffprobe) {
        this.ocr=ocr; this.ffmpeg=ffmpeg; this.ffprobe=ffprobe;
    }

    @Override public ExtractionResult sampleVideo(String fileName, byte[] content) {
        Path dir=null;
        try {
            dir=Files.createTempDirectory("resolveiq-video-"); Path input=dir.resolve("input.mp4"); Files.write(input,content);
            double duration=probe(input); if(duration>300) throw new IllegalArgumentException("Video exceeds five-minute processing limit");
            run(List.of(ffmpeg,"-nostdin","-v","error","-i",input.toString(),"-vf","fps=1,scale=min(1280\\,iw):-2",dir.resolve("frame-%05d.png").toString()),300);
            List<Path> frames; try(var stream=Files.list(dir)){frames=stream.filter(p->p.getFileName().toString().startsWith("frame-")).sorted().limit(300).toList();}
            for(int i=0;i<frames.size();i++) {
                ExtractionResult frame=ocr.processImage(frames.get(i).getFileName().toString(),Files.readAllBytes(frames.get(i)));
                Optional<ObservationData> error=frame.observations().stream().filter(o->o.type()==ObservationType.ERROR_CODE).findFirst();
                if(error.isPresent()) {
                    double second=i; String code=String.format(Locale.ROOT,"%02d:%02d",(int)second/60,(int)second%60);
                    List<ObservationData> observations=new ArrayList<>(frame.observations());
                    observations.add(new ObservationData(ObservationType.FAILURE_TIMESTAMP,code,"Failure frame detected by OCR",error.get().confidence(),"{\"timestamp\":"+second+",\"frame\":"+(i+1)+"}"));
                    return new ExtractionResult(ArtifactType.VIDEO_FRAME,frame.redactedContent(),observations,frame.redactions(),i+1,second);
                }
            }
            return new ExtractionResult(ArtifactType.VIDEO_FRAME,"[NO_FAILURE_FRAME_DETECTED]",List.of(),List.of(),null,null);
        } catch(java.io.IOException error){throw new IllegalStateException("VIDEO_TOOL_UNAVAILABLE: ffmpeg could not process media",error);}
        finally { if(dir!=null) deleteTree(dir); }
    }

    private double probe(Path input) throws java.io.IOException {
        String output=run(List.of(ffprobe,"-v","error","-show_entries","format=duration","-of","default=noprint_wrappers=1:nokey=1",input.toString()),30);
        try{return Double.parseDouble(output.trim());}catch(NumberFormatException e){throw new IllegalArgumentException("Unable to determine video duration",e);}
    }
    private String run(List<String> command,int timeoutSeconds) throws java.io.IOException {
        try { Process process=new ProcessBuilder(command).redirectErrorStream(true).start(); if(!process.waitFor(Duration.ofSeconds(timeoutSeconds).toMillis(),TimeUnit.MILLISECONDS)){process.destroyForcibly();throw new IllegalStateException("Media tool timed out");}
            String output=new String(process.getInputStream().readAllBytes(),StandardCharsets.UTF_8);if(process.exitValue()!=0)throw new IllegalStateException("Media tool failed: "+(output.length()>500?output.substring(0,500):output));return output;
        } catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("Media processing interrupted",e);}
    }
    private void deleteTree(Path root){try(var paths=Files.walk(root)){paths.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(java.io.IOException ignored){}});}catch(java.io.IOException ignored){}}
}
