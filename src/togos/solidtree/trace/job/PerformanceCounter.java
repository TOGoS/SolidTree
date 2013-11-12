package togos.solidtree.trace.job;

public class PerformanceCounter
{
	public volatile long sampleCount;
	public volatile long recentSampleCount;
	public volatile long beginTime = -1;
	public volatile long recentBeginTime = -1;
	public volatile long time = -1;
	
	public void samplesCompleted( long count ) {
		long currentTime = System.currentTimeMillis();
		synchronized(this) {
			if( beginTime == -1 ) beginTime = currentTime;
			if( recentBeginTime == -1 ) recentBeginTime = currentTime;
			sampleCount += count;
			// Every 20 seconds restart the recent samples/second counter
			if( currentTime - recentBeginTime > 20*1000 ) {
				recentBeginTime = time;
				recentSampleCount = count;
			} else {
				recentSampleCount += count;
			}
			
			time = currentTime;
		}
	}
	
	public static String HEADER_STRING = String.format("%15s ; %12s ; %12s", "Samples", "Recent Rate", "Overall Rate");
	
	public String toString() {
		return String.format("% 15d ; % 12.2f ; % 12.2f", sampleCount,
			(time - recentBeginTime == 0) ? 0 : recentSampleCount * 1000d / (time - recentBeginTime),
			(time - beginTime == 0) ? 0 : sampleCount * 1000d / (time - beginTime)
		);
	}
}
