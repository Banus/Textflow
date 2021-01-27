"""Script to filter a list of sentences based on eintailment/contradiction.
Put one sentence per line in a file called 'repository_clean'.

You need to install pytorch and faiseq to run the code.
"""
from itertools import islice

import numpy as np
import torch
from fairseq.data.data_utils import collate_tokens


def pair_class(pairs):
    roberta = torch.hub.load('pytorch/fairseq', 'roberta.large.mnli')
    roberta.eval()
    batch_size = 100

    predictions, confidences = [], []
    for i in range(0, len(pairs), batch_size):
        batch = collate_tokens([roberta.encode(pair[0], pair[1])
                                for pair in pairs[i:i+batch_size]], pad_idx=1)
        probs = roberta.predict('mnli', batch).detach().numpy()
        predictions.append(probs.argmax(axis=1))
        confidences.append(probs.max(axis=1))
    return np.concatenate(predictions), np.concatenate(confidences)


def samples(n_samples):
    texts = []
    with open('repository_clean', 'r') as fid:
        for line in islice(fid, n_samples):
            texts.append(line.strip())
    return texts


def main():
    # change this sentence to find different entailed sentences
    sentence = 'I am here'
    texts = samples(50000)
    pairs = [(t, sentence) for t in texts]

    predictions, confidences = pair_class(pairs)
    idx = np.argsort(confidences)
    predictions = [predictions[i] for i in idx]
    confidences = [confidences[i] for i in idx]
    texts = [texts[i] for i in idx]
    for pred, conf, text in zip(predictions, confidences, texts):
        if pred == 2:  # entailment; use 0 for contradiction
            print(conf, text)


if __name__ == '__main__':
    main()
