package togos.solidtree.trace.job;

public class PerformanceCounter
{
	public volatile long sampleCount;
	public volatile long beginTime = -1;
	public volatile long time = -1;
	public volatile double samplesPerSecondWeightedAverage;
	public volatile double samplesPerSecondLatest;
	
	public void samplesCompleted( long count ) {
		long currentTime = System.currentTimeMillis();
		synchronized(this) {
			long interval = currentTime - time;
			if( beginTime == -1 ) {
				beginTime = currentTime;
			}
			sampleCount += count;
			if( interval > 0 ) {
				samplesPerSecondLatest = count * 1000d / interval;
				samplesPerSecondWeightedAverage =
					(samplesPerSecondWeightedAverage * 0.8) +
					(samplesPerSecondLatest * 0.2);
			}
			time = currentTime;
		}
	}
	
	public static String HEADER_STRING = String.format("%15s ; %12s ; %12s", "Samples", "Recent Rate", "Overall Rate");
	
	public String toString() {
		return String.format("% 15d ; % 12.2f ; % 12.2f", sampleCount, samplesPerSecondWeightedAverage,
			(time - beginTime == 0) ? 0 : sampleCount * 1000d / (time - beginTime) );
	}
}
