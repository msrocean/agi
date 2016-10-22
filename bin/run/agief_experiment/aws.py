import boto3
import subprocess
import os
import botocore

import utils

log = False
cluster = 'default'


# assumes there exists a private key for the given ec2 instance, at ~/.ssh/ecs-key
def sync_experiment(host, keypath):
    print "....... Syncing code to ec2 container instance"

    # code
    file_path = utils.filepath_from_env_variable("", "AGI_HOME")
    cmd = "rsync -ave 'ssh -i " + keypath + "  -o \"StrictHostKeyChecking no\" ' " + file_path + " ec2-user@" + host + ":~/agief-project/agi --exclude={\"*.git/*\",*/src/*}"
    if log:
        print cmd
    output, error = subprocess.Popen(cmd,
                                     shell=True,
                                     stdout=subprocess.PIPE,
                                     stderr=subprocess.PIPE,
                                     executable="/bin/bash").communicate()
    if log:
        print output
        print error

    # experiments
    file_path = utils.filepath_from_env_variable("", "AGI_RUN_HOME")
    cmd = "rsync -ave 'ssh -i " + keypath + "  -o \"StrictHostKeyChecking no\" ' " + file_path + " ec2-user@" + host + ":~/agief-project/run --exclude={\"*.git/*\"}"
    if log:
        print cmd
    output, error = subprocess.Popen(cmd,
                                     shell=True,
                                     stdout=subprocess.PIPE,
                                     stderr=subprocess.PIPE,
                                     executable="/bin/bash").communicate()
    if log:
        print output
        print error


def run_task(task_name):
    """ Run task 'task_name' and return the Task ARN """

    print "....... Running task on ecs "
    client = boto3.client('ecs')
    response = client.run_task(
        cluster=cluster,
        taskDefinition=task_name,
        count=1,
        startedBy='pyScript'
    )

    if log:
        print "LOG: ", response

    length = len(response['failures'])
    if length > 0:
        print "ERROR: could not initiate task on AWS."
        print "reason = " + response['failures'][0]['reason']
        print "arn = " + response['failures'][0]['arn']
        print " ----- exiting -------"
        exit()

    if len(response['tasks']) <= 0:
        print "ERROR: could not retrieve task arn when initiating task on AWS - something has gone wrong."
        exit()

    task_arn = response['tasks'][0]['taskArn']
    return task_arn


def stop_task(task_arn):

    print "....... Stopping task on ecs "
    client = boto3.client('ecs')

    response = client.stop_task(
        cluster=cluster,
        task=task_arn,
        reason='pyScript said so!'
    )

    if log:
        print "LOG: ", response


# run the chosen instance specified by instance_id
# returns the aws public and private ip addresses
def run_ec2(instance_id):
    print "....... Starting ec2 (instance id " + instance_id + ")"
    ec2 = boto3.resource('ec2')
    instance = ec2.Instance(instance_id)
    response = instance.start()

    if log:
        print "LOG: Start response: ", response

    instance.wait_until_running()

    ip_public = instance.public_ip_address
    ip_private = instance.private_ip_address

    print "Instance is up and running."
    print "Instance public IP address is: ", ip_public
    print "Instance private IP address is: ", ip_private

    return {'ip_public': ip_public, 'ip_private': ip_private}


def close(instance_id):
    print "...... Closing ec2 instance (instance id " + instance_id + ")"
    ec2 = boto3.resource('ec2')
    instance = ec2.Instance(instance_id)

    ip_public = instance.public_ip_address
    ip_private = instance.private_ip_address

    print "Instance public IP address is: ", ip_public
    print "Instance private IP address is: ", ip_private

    response = instance.stop()

    if log:
        print "LOG: stop ec2: ", response


def upload_experiment_file(prefix, filename, file_path):
    print "...... Uploading experiment file to S3"

    if not os.path.isfile(file_path):
        print "ERROR: the file: " + file_path + ", DOES NOT EXIST!"
        return

    s3 = boto3.resource('s3')
    bucket_name = "agief-project"

    exists = True
    try:
        s3.meta.client.head_bucket(Bucket=bucket_name)
    except botocore.exceptions.ClientError as e:
        # If a client error is thrown, then check that it was a 404 error.
        # If it was a 404 error, then the bucket does not exist.
        error_code = int(e.response['Error']['Code'])
        if error_code == 404:
            exists = False

    if not exists:
        print "WARNING: s3 bucket " + bucket_name + " does not exist, creating it now."
        s3.create_bucket(Bucket=bucket_name)

    key = "experiment-output/" + prefix + "/" + filename

    print " ... file = " + file_path + ", to bucket = " + bucket_name + ", key = " + key
    response = s3.Object(bucket_name=bucket_name, key=key).put(Body=open(file_path, 'rb'))

    if log:
        print response