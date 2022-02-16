const float RAND_MAX = 32767.0;

float init_weight() {
    return rand() / 32767.0;
}

void shuffle(int array[], int size)
{
    if (size > 1) {
        int i;
        for (i = 0; i < size - 1; i = i + 1) {
            int j, t;
            j = i + (int)(rand() / (32767.0 / (size - i) + 1));
            t = array[j];
            array [j] = array[i];
            array[i] = t;
        }
    }
}

array nn_lrelu(int numTrainingSets, float output[], float training_inputs[][], float training_outputs[][]) {
    const int numInputs = 2;
    const int numHiddenNodes = 5;
    const int numOutputs = 1;
    const float lr = 0.1;
    int i, j, k, n, x;
    float activation;

    // declare the matrices
    float hiddenLayer[numHiddenNodes];

    float hiddenLayerBias[numHiddenNodes];
    float outputLayerBias[numOutputs];

    float hiddenWeights[numInputs][numHiddenNodes];
    float outputWeights[numHiddenNodes][numOutputs];

    float outputLayer[numOutputs];

    // declare training data
    int trainingSetOrder[numTrainingSets];
    for (i=0; i<numTrainingSets; i = i + 1) {
        trainingSetOrder[i] = i;
    }

    // randomly init the NN
    for (i=0; i<numInputs; i = i + 1) {
        for (j=0; j<numHiddenNodes; j = j + 1) {
            hiddenWeights[i][j] = init_weight();
        }
    }
    for (i=0; i<numHiddenNodes; i = i + 1) {
        hiddenLayerBias[i] = init_weight();
        for (j=0; j<numOutputs; j = j + 1) {
            outputWeights[i][j] = init_weight();
        }
    }
    for (i=0; i<numOutputs; i = i + 1) {
        outputLayerBias[i] = init_weight();
    }

    // train
    for (n = 0; n < 1000; n = n + 1) {
        shuffle(trainingSetOrder, numTrainingSets);

        // train for each training value
        for (x = 0; x < numTrainingSets; x = x + 1) {
            i = trainingSetOrder[x];

            // forward pass
            for (j = 0; j < numHiddenNodes; j = j + 1) {
                activation = hiddenLayerBias[j];
                for (k = 0; k < numInputs; k = k + 1) {
                    activation = activation + training_inputs[i][k] * hiddenWeights[k][j];
                }
                if (activation * 0.1 < activation) {
                    hiddenLayer[j] = activation;
                } else {
                    hiddenLayer[j] = 0.1 * activation;
                }
            }
            for (j = 0; j < numOutputs; j = j + 1) {
                activation = outputLayerBias[j];
                for (k = 0; k < numHiddenNodes; k = k + 1) {
                    activation = activation + hiddenLayer[k] * outputWeights[k][j];
                }
                if (activation * 0.1 < activation) {
                    outputLayer[j] = activation;
                } else {
                    outputLayer[j] = 0.1 * activation;
                }
            }

            // backwards propagation
            float deltaOutput[numOutputs];
            for (j = 0; j < numOutputs; j = j + 1) {
                float errorOutput;
                errorOutput = training_outputs[i][j] - outputLayer[j];
                if (outputLayer[j] > 0) {
                    deltaOutput[j] = errorOutput;
                } else {
                    deltaOutput[j] = errorOutput * 0.01;
                }
            }

            float deltaHidden[numHiddenNodes];
            for (j = 0; j < numHiddenNodes; j = j + 1) {
                float errorHidden;
                errorHidden = 0.0;
                for (k = 0; k < numOutputs; k = k + 1) {
                    errorHidden = errorHidden + deltaOutput[k] * outputWeights[j][k];
                }
                if (hiddenLayer[j] > 0) {
                    deltaHidden[j] = errorHidden;
                } else {
                    deltaHidden[j] = errorHidden* 0.01;
                }
            }

            for (j = 0; j < numOutputs; j = j + 1) {
                outputLayerBias[j] = outputLayerBias[j] + deltaOutput[j] * lr;
                for (k = 0; k < numHiddenNodes; k = k + 1) {
                    outputWeights[k][j] = outputWeights[k][j] + hiddenLayer[k]*deltaOutput[j]*lr;
                }
            }

            for (j = 0; j < numHiddenNodes; j = j + 1) {
                hiddenLayerBias[j] = hiddenLayerBias[j] + deltaHidden[j]*lr;
                for(k = 0; k<numInputs; k = k + 1) {
                    hiddenWeights[k][j] = hiddenWeights[k][j] + training_inputs[i][k]*deltaHidden[j]*lr;
                }
            }
        }
    }

    // validate results
    for (x = 0; x < numTrainingSets; x = x + 1) {
        // forward pass
        for (j = 0; j < numHiddenNodes; j = j + 1) {
            activation = hiddenLayerBias[j];
            for (k = 0; k < numInputs; k = k + 1) {
                activation = activation + training_inputs[x][k] * hiddenWeights[k][j];
            }
            if (activation * 0.1 < activation) {
                hiddenLayer[j] = activation;
            } else {
                hiddenLayer[j] = 0.1 * activation;
            }
        }
        for (j = 0; j < numOutputs; j = j + 1) {
            activation = outputLayerBias[j];
            for (k = 0; k < numHiddenNodes; k = k + 1) {
                activation = activation + hiddenLayer[k] * outputWeights[k][j];
            }
            if (activation * 0.1 < activation) {
                outputLayer[j] = activation;
            } else {
                outputLayer[j] = 0.1 * activation;
            }
            output[x] = outputLayer[j];
        }
    }

    return output;
}


array nn_entry(int numTrainingSets, float training_inputs_raw[], float training_outputs_raw[]) {
    const int numOutputs = 1;
    const int numInputs = 2;

    float output[numTrainingSets];
    int i, j;

    // transform training inputs to multidimensional array
    float training_inputs[numTrainingSets][numInputs];
    float training_outputs[numTrainingSets][numOutputs];
    j = 0;
    for (i = 0; i < numTrainingSets; i = i + 1) {
        training_inputs[i][0] = training_inputs_raw[j];
        j = j + 1;
        training_inputs[i][1] = training_inputs_raw[j];
        j = j + 1;
        training_outputs[i][0] = training_outputs_raw[i];
    }

    nn_lrelu(numTrainingSets, output, training_inputs, training_outputs);

    for (i = 0; i < numTrainingSets; i = i + 1) {
        print("Input ");
        print(training_inputs[i][0]);
        print(training_inputs[i][1]);

        print("Output ");
        print(output[i]);

        print("Expected output ");
        print(training_outputs[i][0]);
        print("");
    }

    return output;
}