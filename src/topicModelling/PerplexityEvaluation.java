package topicModelling;

import java.io.PrintStream;
import Util.Randoms;

/**
 *
 * @author Lahari
 * @created on Nov 18, 2015
 */
public class PerplexityEvaluation {
    
    int numTopics;
    int[][] wordTopicCounts;
    private double beta;
    private double betaSum;
    private int[] tokensPerTopic;
    private double[] alpha;
    private double[] cachedCoefficients;
    private int topicMask;
    private int topicBits;
    private double smoothingOnlyMass;
    private double alphaSum;
    private Randoms random;

    public PerplexityEvaluation(int numTopics,double[] alpha, double alphaSum,
								  double beta,
								  int[][] typeTopicCounts, 
								  int[] tokensPerTopic) {
        		this.numTopics = numTopics;

		if (Integer.bitCount(numTopics) == 1) {
			// exact power of 2
			topicMask = numTopics - 1;
			topicBits = Integer.bitCount(topicMask);
		}
		else {
			// otherwise add an extra bit
			topicMask = Integer.highestOneBit(numTopics) * 2 - 1;
			topicBits = Integer.bitCount(topicMask);
		}

		this.wordTopicCounts = typeTopicCounts;
		this.tokensPerTopic = tokensPerTopic;
		
		this.alphaSum = alphaSum;
		this.alpha = alpha;
		this.beta = beta;
		this.betaSum = beta * typeTopicCounts.length;
		this.random = new Randoms();
		
		cachedCoefficients = new double[ numTopics ];

		// Initialize the smoothing-only sampling bucket
		smoothingOnlyMass = 0;
		
		// Initialize the cached coefficients, using only smoothing.
		//  These values will be selectively replaced in documents with
		//  non-zero counts in particular topics.
		
		for (int topic=0; topic < numTopics; topic++) {
			smoothingOnlyMass += alpha[topic] * beta / (tokensPerTopic[topic] + betaSum);
			cachedCoefficients[topic] =  alpha[topic] / (tokensPerTopic[topic] + betaSum);
		}
		
		System.err.println("Topic Evaluator: " + numTopics + " topics, " +typeTopicCounts.length+ " tokens, "+topicBits + " topic bits, " + 
						   Integer.toBinaryString(topicMask) + " topic mask");
    }
    
    
    

    public double evaluateLeftToRight(int[][] testing, int numParticles, boolean usingResampling) {
        
        random = new Randoms();

        double logNumParticles = Math.log(numParticles);
        double totalLogLikelihood = 0;
        int totalLength = 0;
        for (int[] tokenSequence : testing) {
            double docLogLikelihood = 0;

            double[][] particleProbabilities = new double[numParticles][];
            for (int particle = 0; particle < numParticles; particle++) {
                particleProbabilities[particle]
                        = leftToRight(tokenSequence, usingResampling);
            }

            for (int position = 0; position < particleProbabilities[0].length; position++) {
                double sum = 0;
                for (int particle = 0; particle < numParticles; particle++) {
                    sum += particleProbabilities[particle][position];
                }
                if (sum > 0.0) {
                    docLogLikelihood += Math.log(sum) - logNumParticles;
                    //docLogLikelihood /= tokenSequence.length;
                }
            }
            totalLogLikelihood += docLogLikelihood;
            totalLength += tokenSequence.length;
        }

        double perplexity = Math.exp(-totalLogLikelihood/totalLength);
        return perplexity;
    }

    protected double[] leftToRight(int[] tokenSequence, boolean usingResampling) {

        int[] oneDocTopics = new int[tokenSequence.length];
        double[] wordProbabilities = new double[tokenSequence.length];

        int[] currentTypeTopicCounts;
        int wordToken, oldTopic, newTopic;
        double topicWeightsSum;
        int docLength = tokenSequence.length;

		// Keep track of the number of tokens we've examined, not
        //  including out-of-vocabulary words
        int tokensSoFar = 0;

        int[] localTopicCounts = new int[numTopics];
        int[] localTopicIndex = new int[numTopics];

		// Build an array that densely lists the topics that
        //  have non-zero counts.
        int denseIndex = 0;

        // Record the total number of non-zero topics
        int nonZeroTopics = denseIndex;

        //		Initialize the topic count/beta sampling bucket
        double topicBetaMass = 0.0;
        double topicTermMass = 0.0;

        double[] topicTermScores = new double[numTopics];
        int[] topicTermIndices;
        int[] topicTermValues;
        int i;
        double score;

        double logLikelihood = 0;

		// All counts are now zero, we are starting completely fresh.
        //	Iterate over the positions (words) in the document 
        for (int limit = 0; limit < docLength; limit++) {

			// Record the marginal probability of the token
            //  at the current limit, summed over all topics.
            if (usingResampling) {

                // Iterate up to the current limit
                for (int position = 0; position < limit; position++) {
                    wordToken = tokenSequence[position];
                    oldTopic = oneDocTopics[position];

                    // Check for out-of-vocabulary words
                    if (wordToken >= wordTopicCounts.length
                            || wordTopicCounts[wordToken] == null) {
                        continue;
                    }
                    currentTypeTopicCounts = wordTopicCounts[wordToken];

                    //	Remove this token from all counts. 
                    // Remove this topic's contribution to the normalizing constants.
                    // Note that we are using clamped estimates of P(w|t),so we are NOT changing smoothingOnlyMass.
                    topicBetaMass -= beta * localTopicCounts[oldTopic]
                            / (tokensPerTopic[oldTopic] + betaSum);

                    // Decrement the local doc/topic counts
                    localTopicCounts[oldTopic]--;

                    // Maintain the dense index, if we are deleting the old topic
                    if (localTopicCounts[oldTopic] == 0) {
                        // First get to the dense location associated with the old topic.
                        denseIndex = 0;
                        // We know it's in there somewhere, so we don't need bounds checking.
                        while (localTopicIndex[denseIndex] != oldTopic) {
                            denseIndex++;
                        }
                        // shift all remaining dense indices to the left.
                        while (denseIndex < nonZeroTopics) {
                            if (denseIndex < localTopicIndex.length - 1) {
                                localTopicIndex[denseIndex]
                                        = localTopicIndex[denseIndex + 1];
                            }
                            denseIndex++;
                        }
                        nonZeroTopics--;
                    }

                    // Add the old topic's contribution back into the normalizing constants.
                    topicBetaMass += beta * localTopicCounts[oldTopic]
                            / (tokensPerTopic[oldTopic] + betaSum);

                    // Reset the cached coefficient for this topic
                    cachedCoefficients[oldTopic]
                            = (alpha[oldTopic] + localTopicCounts[oldTopic])
                            / (tokensPerTopic[oldTopic] + betaSum);

                    // Now go over the type/topic counts, calculating the score for each topic.
                    int index = 0;
                    int currentTopic, currentValue;

                    boolean alreadyDecremented = false;

                    topicTermMass = 0.0;

                    while (index < currentTypeTopicCounts.length
                            && currentTypeTopicCounts[index] > 0) {
                        currentTopic = currentTypeTopicCounts[index] & topicMask;
                        currentValue = currentTypeTopicCounts[index] >> topicBits;

                        score
                                = cachedCoefficients[currentTopic] * currentValue;
                        topicTermMass += score;
                        topicTermScores[index] = score;

                        index++;
                    }

                    double sample = random.nextUniform() * (smoothingOnlyMass + topicBetaMass + topicTermMass);
                    double origSample = sample;

                    //	Make sure it actually gets set
                    newTopic = -1;

                    if (sample < topicTermMass) {
                        i = -1;
                        while (sample > 0) {
                            i++;
                            sample -= topicTermScores[i];
                        }
                        newTopic = currentTypeTopicCounts[i] & topicMask;
                    } else {
                        sample -= topicTermMass;
                        if (sample < topicBetaMass) {
                            //betaTopicCount++;
                            sample /= beta;
                            for (denseIndex = 0; denseIndex < nonZeroTopics; denseIndex++) {
                                int topic = localTopicIndex[denseIndex];
                                sample -= localTopicCounts[topic]
                                        / (tokensPerTopic[topic] + betaSum);
                                if (sample <= 0.0) {
                                    newTopic = topic;
                                    break;
                                }
                            }
                        } else {
                            //smoothingOnlyCount++;
                            sample -= topicBetaMass;
                            sample /= beta;
                            newTopic = 0;
                            sample -= alpha[newTopic]
                                    / (tokensPerTopic[newTopic] + betaSum);
                            while (sample > 0.0) {
                                newTopic++;
                                sample -= alpha[newTopic]
                                        / (tokensPerTopic[newTopic] + betaSum);
                            }
                        }
                    }
                    if (newTopic == -1) {
                        System.err.println("sampling error: " + origSample + " " + sample + " " + smoothingOnlyMass + " "
                                + topicBetaMass + " " + topicTermMass);
                        newTopic = numTopics - 1; // TODO is this appropriate
                        //throw new IllegalStateException ("WorkerRunnable: New topic not sampled.");
                    }
					//assert(newTopic != -1);
                    //			Put that new topic into the counts
                    oneDocTopics[position] = newTopic;
                    topicBetaMass -= beta * localTopicCounts[newTopic]
                            / (tokensPerTopic[newTopic] + betaSum);
                    localTopicCounts[newTopic]++;

                    // If this is a new topic for this document, add the topic to the dense index.
                    if (localTopicCounts[newTopic] == 1) {

                        // First find the point where we 
                        //  should insert the new topic by going to
                        //  the end (which is the only reason we're keeping
                        //  track of the number of non-zero
                        //  topics) and working backwards
                        denseIndex = nonZeroTopics;

                        while (denseIndex > 0
                                && localTopicIndex[denseIndex - 1] > newTopic) {

                            localTopicIndex[denseIndex]
                                    = localTopicIndex[denseIndex - 1];
                            denseIndex--;
                        }

                        localTopicIndex[denseIndex] = newTopic;
                        nonZeroTopics++;
                    }

                    //	update the coefficients for the non-zero topics
                    cachedCoefficients[newTopic]
                            = (alpha[newTopic] + localTopicCounts[newTopic])
                            / (tokensPerTopic[newTopic] + betaSum);

                    topicBetaMass += beta * localTopicCounts[newTopic]
                            / (tokensPerTopic[newTopic] + betaSum);

                }
            }

            // We've just resampled all tokens UP TO the current limit, now sample the token AT the current limit.
            wordToken = tokenSequence[limit];

            // Check for out-of-vocabulary words
            if (wordToken >= wordTopicCounts.length
                    || wordTopicCounts[wordToken] == null) {
                continue;
            }

            currentTypeTopicCounts = wordTopicCounts[wordToken];

            int index = 0;
            int currentTopic, currentValue;

            topicTermMass = 0.0;

            while (index < currentTypeTopicCounts.length
                    && currentTypeTopicCounts[index] > 0) {
                currentTopic = currentTypeTopicCounts[index] & topicMask;
                currentValue = currentTypeTopicCounts[index] >> topicBits;

                score
                        = cachedCoefficients[currentTopic] * currentValue;
                topicTermMass += score;
                topicTermScores[index] = score;

				//System.out.println("  " + currentTopic + " = " + currentValue);
                index++;
            }

           
            double sample = random.nextUniform() * (smoothingOnlyMass + topicBetaMass + topicTermMass);
            double origSample = sample;

			// Note that we've been absorbing (alphaSum + docLength) into
            //  the normalizing constant. The true marginal probability needs
            //  this term, so we stick it back in.
            wordProbabilities[limit]
                    += (smoothingOnlyMass + topicBetaMass + topicTermMass)
                    / (alphaSum + tokensSoFar);

            //System.out.println("normalizer: " + alphaSum + " + " + tokensSoFar);
            tokensSoFar++;

            //	Make sure it actually gets set
            newTopic = -1;

            if (sample < topicTermMass) {

                i = -1;
                while (sample > 0) {
                    i++;
                    sample -= topicTermScores[i];
                }

                newTopic = currentTypeTopicCounts[i] & topicMask;
            } else {
                sample -= topicTermMass;

                if (sample < topicBetaMass) {
					//betaTopicCount++;

                    sample /= beta;

                    for (denseIndex = 0; denseIndex < nonZeroTopics; denseIndex++) {
                        int topic = localTopicIndex[denseIndex];

                        sample -= localTopicCounts[topic]
                                / (tokensPerTopic[topic] + betaSum);

                        if (sample <= 0.0) {
                            newTopic = topic;
                            break;
                        }
                    }

                } else {
					//smoothingOnlyCount++;

                    sample -= topicBetaMass;

                    sample /= beta;

                    newTopic = 0;
                    sample -= alpha[newTopic]
                            / (tokensPerTopic[newTopic] + betaSum);

                    while (sample > 0.0) {
                        newTopic++;
                        sample -= alpha[newTopic]
                                / (tokensPerTopic[newTopic] + betaSum);
                    }

                }

            }

            if (newTopic == -1) {
                System.err.println("sampling error: " + origSample + " "
                        + sample + " " + smoothingOnlyMass + " "
                        + topicBetaMass + " " + topicTermMass);
                newTopic = numTopics - 1; // TODO is this appropriate
            }

            // Put that new topic into the counts
            oneDocTopics[limit] = newTopic;

            topicBetaMass -= beta * localTopicCounts[newTopic]
                    / (tokensPerTopic[newTopic] + betaSum);

            localTopicCounts[newTopic]++;

			// If this is a new topic for this document,
            //  add the topic to the dense index.
            if (localTopicCounts[newTopic] == 1) {
                denseIndex = nonZeroTopics;

                while (denseIndex > 0
                        && localTopicIndex[denseIndex - 1] > newTopic) {

                    localTopicIndex[denseIndex]
                            = localTopicIndex[denseIndex - 1];
                    denseIndex--;
                }

                localTopicIndex[denseIndex] = newTopic;
                nonZeroTopics++;
            }

            //	update the coefficients for the non-zero topics
            cachedCoefficients[newTopic]
                    = (alpha[newTopic] + localTopicCounts[newTopic])
                    / (tokensPerTopic[newTopic] + betaSum);

            topicBetaMass += beta * localTopicCounts[newTopic]
                    / (tokensPerTopic[newTopic] + betaSum);

			//System.out.println(type + "\t" + newTopic + "\t" + logLikelihood);
        }

		//	Clean up our mess: reset the coefficients to values with only
        //	smoothing. The next doc will update its own non-zero topics...
        for (denseIndex = 0; denseIndex < nonZeroTopics; denseIndex++) {
            int topic = localTopicIndex[denseIndex];

            cachedCoefficients[topic]
                    = alpha[topic] / (tokensPerTopic[topic] + betaSum);
        }

        return wordProbabilities;

    }

}
