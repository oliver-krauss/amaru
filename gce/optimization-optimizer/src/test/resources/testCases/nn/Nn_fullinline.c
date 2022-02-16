// NOTE: Fullinline uses SWISH
array nn_fullinline(int numTrainingSets, float output[], float training_inputs[][], float training_outputs[][]) {
    const int numInputs = 2;
    const int numHiddenNodes = 5;
    const int numOutputs = 1;
    const float lr = 0.3;
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
            hiddenWeights[i][j] = rand() / 32767.0;
        }
    }
    for (i=0; i<numHiddenNodes; i = i + 1) {
        hiddenLayerBias[i] = rand() / 32767.0;
        for (j=0; j<numOutputs; j = j + 1) {
            outputWeights[i][j] = rand() / 32767.0;
        }
    }
    for (i=0; i<numOutputs; i = i + 1) {
        outputLayerBias[i] = rand() / 32767.0;
    }

    // train
    for (n = 0; n < 1000; n = n + 1) {
        int i;
        for (i = 0; i < numTrainingSets - 1; i = i + 1) {
            int j, t;
            j = i + (int)(rand() / (32767.0 / (numTrainingSets - i) + 1));
            t = trainingSetOrder[j];
            trainingSetOrder[j] = trainingSetOrder[i];
            trainingSetOrder[i] = t;
        }

        // train for each training value
        for (x = 0; x < numTrainingSets; x = x + 1) {
            i = trainingSetOrder[x];


            // forward pass
            for (j = 0; j < numHiddenNodes; j = j + 1) {
                activation = hiddenLayerBias[j];
                for (k = 0; k < numInputs; k = k + 1) {
                    activation = activation + training_inputs[i][k] * hiddenWeights[k][j];
                }
                hiddenLayer[j] = activation / (1 + exp(-activation));
            }
            for (j = 0; j < numOutputs; j = j + 1) {
                activation = outputLayerBias[j];
                for (k = 0; k < numHiddenNodes; k = k + 1) {
                    activation = activation + hiddenLayer[k] * outputWeights[k][j];
                }
                outputLayer[j] = activation / (1 + exp(-activation));
            }

            // backwards propagation
            float deltaOutput[numOutputs];
            for (j = 0; j < numOutputs; j = j + 1) {
                float errorOutput;
                errorOutput = training_outputs[i][j] - outputLayer[j];
                deltaOutput[j] = errorOutput * ((1 - outputLayer[j]) / (1 + exp(-outputLayer[j])) + outputLayer[j]);
            }

            float deltaHidden[numHiddenNodes];
            for (j = 0; j < numHiddenNodes; j = j + 1) {
                float errorHidden;
                errorHidden = 0.0;
                for (k = 0; k < numOutputs; k = k + 1) {
                    errorHidden = errorHidden + deltaOutput[k] * outputWeights[j][k];
                }
                deltaHidden[j] = errorHidden * ((1 - hiddenLayer[j]) / (1 + exp(-hiddenLayer[j])) + hiddenLayer[j]);
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
            hiddenLayer[j] = activation / (1 + exp(-activation));
        }
        for (j = 0; j < numOutputs; j = j + 1) {
            activation = outputLayerBias[j];
            for (k = 0; k < numHiddenNodes; k = k + 1) {
                activation = activation + hiddenLayer[k] * outputWeights[k][j];
            }
            outputLayer[j] = activation / (1 + exp(-activation));
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

    nn_fullinline(numTrainingSets, output, training_inputs, training_outputs);

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